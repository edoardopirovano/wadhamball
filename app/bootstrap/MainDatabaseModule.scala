package bootstrap

import com.google.inject.AbstractModule

class MainDatabaseModule extends AbstractModule {
  protected def configure(): Unit = {
    bind(classOf[InitialData]).asEagerSingleton()
  }
}
