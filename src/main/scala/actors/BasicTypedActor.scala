package actors

import akka.actor.typed.Behavior

object BasicTypedActor extends App {


  object biggerActor {

    trait maths

    def apply(): Behavior[maths] = ???
  }


}
