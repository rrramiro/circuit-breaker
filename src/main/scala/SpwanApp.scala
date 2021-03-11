import cats.effect.ExitCode
import cats.implicits._
import monix.eval.{Task, TaskApp}
import monix.reactive.Observable

import scala.concurrent.duration._
import scala.io.StdIn

object SpwanApp extends TaskApp {
  val printOne = Task(println("one"))
  val printTow = Task(println("tow"))

  override def run(args: List[String]): Task[ExitCode] =
    Task.suspend {
      {
        for {
          _ <- Observable.interval(2.seconds)
          _ = println("hello")
        } yield ()
      }.completedL.start
        .bracket(_ => Task(StdIn.readLine()))(_.cancel)
        .flatMap(_ => Task(println("\nBye!\n")))
        .as(ExitCode.Success)
    }
}
