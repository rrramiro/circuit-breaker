import cats.effect.{Async, ExitCase, IO, Sync}

object Overrides {

  implicit val syncIO: Sync[IO] = new CustomSyncIO

  implicit val asyncIO: Async[IO] =
    new CustomSyncIO with Async[IO] {
      def async[A](k: (Either[Throwable, A] => Unit) => Unit): IO[A] =
        IO.ioEffect.async(k)
      def asyncF[A](k: (Either[Throwable, A] => Unit) => IO[Unit]): IO[A] =
        IO.ioEffect.asyncF(k)
    }

  class CustomSyncIO extends Sync[IO] {
    def suspend[A](thunk: => IO[A]): IO[A] =
      IO.ioEffect.suspend(thunk)
    def bracketCase[A, B](
        acquire: IO[A]
    )(use: A => IO[B])(release: (A, ExitCase[Throwable]) => IO[Unit]): IO[B] =
      IO.ioEffect.bracketCase(acquire)(use)(release)
    def raiseError[A](e: Throwable): IO[A] =
      IO.ioEffect.raiseError(e)
    def handleErrorWith[A](fa: IO[A])(f: Throwable => IO[A]): IO[A] =
      IO.ioEffect.handleErrorWith(fa)(f)
    def pure[A](x: A): IO[A] =
      IO.ioEffect.pure(x)
    def flatMap[A, B](fa: IO[A])(f: A => IO[B]): IO[B] =
      IO.ioEffect.flatMap(fa)(f)
    def tailRecM[A, B](a: A)(f: A => IO[Either[A, B]]): IO[B] =
      IO.ioEffect.tailRecM(a)(f)
  }
}
