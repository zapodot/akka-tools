package no.nextgentel.oss.akkatools.aggregate

import java.util.UUID

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import no.nextgentel.oss.akkatools.aggregate.testAggregate.StateName
import no.nextgentel.oss.akkatools.aggregate.testAggregate._
import no.nextgentel.oss.akkatools.persistence.{DurableMessageForwardAndConfirm, GetState}
import no.nextgentel.oss.akkatools.testing.{DurableMessageTesting, AggregateStateGetter}
import org.scalatest._
import org.slf4j.LoggerFactory
import DurableMessageTesting._
import StateName._

class GeneralAggregateTest(_system:ActorSystem) extends TestKit(_system) with FunSuiteLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  def this() = this(ActorSystem("test-actor-system", ConfigFactory.load("application-test.conf")))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val log = LoggerFactory.getLogger(getClass)
  private def generateId() = UUID.randomUUID().toString

  var id:String=null
  var ourDispatcher:TestProbe=null
  var printShop:TestProbe=null
  var cinema:TestProbe=null
  var main:ActorRef=null
  var stateGetter:AggregateStateGetter[BookingState]=null

  val seatIds = List("s1","s2", "s3-This-id-is-going-to-be-discarded", "s4")

  before {
    id = generateId()
    ourDispatcher = TestProbe()
    printShop = TestProbe()
    cinema = TestProbe()
    main = system.actorOf(BookingAggregate.props(ourDispatcher.ref.path, DurableMessageForwardAndConfirm(printShop.ref).path, DurableMessageForwardAndConfirm(cinema.ref).path, seatIds), "BookingAggregate-" + id)
    stateGetter = AggregateStateGetter[BookingState](main)
  }


  def assertState(correctState:BookingState): Unit = {
    assert(stateGetter.getState() == correctState)
  }

  test("normal flow") {
    // Make sure we start with empty state
    assertState(BookingState.empty())

    val maxSeats = 2
    val sender = TestProbe()
    // Open the booking
    sendDMBlocking(main, OpenBookingCmd(id, maxSeats), sender.ref)
    assertState(BookingState(OPEN, maxSeats, Set()))

    // send first booking
    sendDMBlocking(main, ReserveSeatCmd(id), sender.ref)
    assertState(BookingState(OPEN, maxSeats, Set("s1")))
    sender.expectMsg("s1") // make sure we got the seatId back

    printShop.expectMsg(PrintTicketMessage("s1")) // make sure the ticket was sent for printing

    // send another booking
    sendDMBlocking(main, ReserveSeatCmd(id), sender.ref)
    assertState(BookingState(OPEN, maxSeats, Set("s1", "s2")))
    sender.expectMsg("s2") // make sure we got the seatId back

    printShop.expectMsg(PrintTicketMessage("s2")) // make sure the ticket was sent for printing

    // make another booking which should fail
    sendDMBlocking(main, ReserveSeatCmd(id), sender.ref)
    // make sure our state has not been changed
    assertState(BookingState(OPEN, maxSeats, Set("s1", "s2")))

    // and we should get an error back to sender
    assert(sender.expectMsgAnyClassOf(classOf[Failure]).cause.getMessage == "No more seats available")

    // Let's cancel the first booking
    sendDMBlocking(main, CancelSeatCmd(id, "s1"), sender.ref)
    assertState(BookingState(OPEN, maxSeats, Set("s2")))
    sender.expectMsg("ok")

    // make another bocking that should work
    sendDMBlocking(main, ReserveSeatCmd(id), sender.ref)
    assertState(BookingState(OPEN, maxSeats, Set("s2", "s4")))
    sender.expectMsg("s4") // make sure we got the seatId back

    printShop.expectMsg(PrintTicketMessage("s4")) // make sure the ticket was sent for printing

    // close the booking
    sendDMBlocking(main, CloseBookingCmd(id), sender.ref)
    assertState(BookingState(CLOSED, maxSeats, Set("s2", "s4")))

    // make sure the the cinema got its notice
    cinema.expectMsg(CinemaNotification(List("s2", "s4")))


    // Make a booking after it has been closed - will fail
    sendDMBlocking(main, ReserveSeatCmd(id), sender.ref)
    // make sure our state has not been changed
    assertState(BookingState(CLOSED, maxSeats, Set("s2", "s4")))

    // and we should get an error back to sender
    assert(sender.expectMsgAnyClassOf(classOf[Failure]).cause.getMessage == "Booking is closed")



  }


}



