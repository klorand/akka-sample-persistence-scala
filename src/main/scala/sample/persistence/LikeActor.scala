package sample.persistence

//#persistent-actor-example
import akka.actor._
import akka.persistence._

case class LikeCmd(firstId: String, secondId:String)
case class LikeEvt(data: String)

case class LikeState(likedId:String, events: List[String] = Nil) {
  def updated(evt: LikeEvt): LikeState = copy(likedId, evt.data :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}

class LikePersistentActor(likedId: String) extends PersistentActor {
  override def persistenceId = likedId

  var state = LikeState(likedId)

  def updateState(event: LikeEvt): Unit =
    state = state.updated(event)

  def numEvents =
    state.size

  val receiveRecover: Receive = {
    case evt: LikeEvt                                 => updateState(evt)
    case SnapshotOffer(_, snapshot: LikeState) => state = snapshot
  }

  val receiveCommand: Receive = {
    case LikeCmd(first, second) if second==likedId =>
      persist(LikeEvt(s"${first}"))(updateState)
    case "snap"  => saveSnapshot(state)
    case "print" => println(state)
    case a => throw new Exception(String.valueOf(a));
  }

}
//#persistent-actor-example

object LikeActor extends App {

  val system = ActorSystem("like")
  val persistentActorB = system.actorOf(Props(new LikePersistentActor("b")), "persistentActor-b")
  val persistentActorA = system.actorOf(Props(new LikePersistentActor("a")), "persistentActor-a")

  persistentActorB ! LikeCmd("a","b")
  persistentActorB ! LikeCmd("c","b")
  persistentActorB ! LikeCmd("d","b")
  persistentActorA ! LikeCmd("b","a")
  persistentActorA ! LikeCmd("c","a")
  persistentActorB ! "snap"
  persistentActorA ! "snap"
  persistentActorA ! LikeCmd("snapped","a")
  persistentActorB ! LikeCmd("snapped","b")
  persistentActorB ! "print"
  persistentActorA ! "print"
//  persistentActor ! Cmd("baz")
//  persistentActor ! Cmd("bar")
//  persistentActor ! "snap"
//  persistentActor ! Cmd("buzz")
//  persistentActor ! "print"

  Thread.sleep(10000)
  system.shutdown()
}
