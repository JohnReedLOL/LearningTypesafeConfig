import com.typesafe.config._
import simplelib._

object ComplexApp extends App {

    // This app is "complex" because we load multiple separate app
    // configs into a single JVM and we have a separately-configurable
    // context for simple lib.

    // using a custom Config with the simple-lib library
    // (simple-lib is a library in this same examples/ directory)
    def demoConfigInSimpleLib(config: Config) {
        val context = new SimpleLibContext(config)
        context.printSetting("simple-lib.foo") // "This value comes from complex-app's complex1.conf"
        context.printSetting("simple-lib.hello") // "This value comes from simple-lib's reference.conf"
        context.printSetting("simple-lib.whatever") // "This value comes from a system property"
    }

    // system property overrides work, but the properties must be set
    // before the config lib is used (config lib will not notice changes
    // once it loads the properties)
    System.setProperty("simple-lib.whatever", "This value comes from a system property")

    ///////////

    // "config1" is just an example of using a file other than application.conf
    val config1: Config = ConfigFactory.load("complex1")
    // this.getClass.getClassLoader.getResource("application")
    // Thread.currentThread().getContextClassLoader.getResource("application")
    /*
    complex-app.something = "This value comes from complex-app's complex1.conf"
    simple-lib.foo = "This value comes from complex-app's complex1.conf"
    simple-lib.hello = "This value comes from simple-lib's reference.conf"
    simple-lib.whatever = "This value comes from a system property"
     */

    // use the config ourselves
    println("config1, complex-app.something=" + config1.getString("complex-app.something"))
    // "config1, complex-app.something=This value comes from complex-app's complex1.conf"

    // use the config for a library
    demoConfigInSimpleLib(config1)

    //////////

    // "config2" shows how to configure a library with a custom settings subtree
    val config2: Config = ConfigFactory.load("complex2")
    /*
    simple-lib.foo = "This value comes from simple-lib's reference.conf"
    simple-lib.hello = "This value comes from simple-lib's reference.conf"
    simple-lib.whatever = "This value comes from a system property"
    complex.something = "This value comes from complex-app's complex2.conf"
    complex.simple-lib-context.simple-lib.foo = "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
    complex.simple-lib-context.simple-lib.whatever = "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
    */

    // use the config ourselves
    println("config2, complex-app.something=" + config2.getString("complex-app.something"))
    // "config2, complex-app.something=This value comes from complex-app's complex2.conf")

    // pull out complex-app.simple-lib-context and move it to
    // the toplevel, creating a new config suitable for our SimpleLibContext.
    // The defaultOverrides() have to be put back on top of the stack so
    // they still override any simple-lib settings.
    // We fall back to config2 again to be sure we get simple-lib's
    // reference.conf plus any other settings we've set. You could
    // also just fall back to ConfigFactory.referenceConfig() if
    // you don't want complex2.conf settings outside of
    // complex-app.simple-lib-context to be used.
    val simpleLibConfig2 = ConfigFactory.defaultOverrides()
        .withFallback(config2.getConfig("complex-app.simple-lib-context"))
        .withFallback(config2)
    /*
    simple-lib.foo = "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
    simple-lib.hello = "This value comes from simple-lib's reference.conf"
    simple-lib.whatever = "This value comes from a system property"
    complex.something = "This value comes from complex-app's complex2.conf"
    complex.simple-lib-context.simple-lib.foo = "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
    complex.simple-lib-context.simple-lib.whatever = "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
    */

    demoConfigInSimpleLib(simpleLibConfig2)
    /*
    context.printSetting("simple-lib.foo") // "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
    context.printSetting("simple-lib.hello") // "This value comes from simple-lib's reference.conf"
    context.printSetting("simple-lib.whatever") // "This value comes from a system property"
     */

    //////////

    // Here's an illustration that simple-lib will get upset if we pass it
    // a bad config. In this case, we'll fail to merge the reference
    // config in to complex-app.simple-lib-context, so simple-lib will
    // point out that some settings are missing.
    try {
        val configToValidate: Config = config2.getConfig("complex-app.simple-lib-context")
        demoConfigInSimpleLib(configToValidate)
        /*
        simple-lib.foo = "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
        simple-lib.whatever = "This value comes from complex-app's complex2.conf in its custom simple-lib-context"

        /*
        config.checkValid(ConfigFactory.defaultReference(), "simple-lib")

        simple-lib.foo = "This value comes from simple-lib's reference.conf" // validated
        simple-lib.hello = "This value comes from simple-lib's reference.conf" // NOT VALIDATED
        simple-lib.whatever = "This value comes from simple-lib's reference.conf" // validated
        override with system property:
        simple-lib.whatever = "This value comes from a system property"
        */

        // This code is never reached:
        context.printSetting("simple-lib.foo") // "This value comes from complex-app's complex2.conf in its custom simple-lib-context"
        context.printSetting("simple-lib.hello") // Exception
        context.printSetting("simple-lib.whatever")
         */
    } catch {
        // validation issue
        case e: ConfigException.ValidationFailed => // From config.checkValid(ConfigFactory.defaultReference(), "simple-lib")
            println("when we passed a bad config to simple-lib, it said: " + e.getMessage) // simple-lib.hello not validated.
    }
}
/*
Expected output:

config1, complex-app.something=This value comes from complex-app's complex1.conf
The setting 'simple-lib.foo' is: This value comes from complex-app's complex1.conf
The setting 'simple-lib.hello' is: This value comes from simple-lib's reference.conf
The setting 'simple-lib.whatever' is: This value comes from a system property
config2, complex-app.something=This value comes from complex-app's complex2.conf
The setting 'simple-lib.foo' is: This value comes from complex-app's complex2.conf in its custom simple-lib-context
The setting 'simple-lib.hello' is: This value comes from simple-lib's reference.conf
The setting 'simple-lib.whatever' is: This value comes from a system property
// This setting is missing: simple-lib.hello
// NOT VALIDATED: simple-lib.hello
when we passed a bad config to simple-lib, it said: simple-lib.hello

config1, complex-app.something=This value comes from complex-app's complex1.conf
The setting 'simple-lib.foo' is: This value comes from complex-app's complex1.conf
The setting 'simple-lib.hello' is: This value comes from simple-lib's reference.conf
The setting 'simple-lib.whatever' is: This value comes from a system property
config2, complex-app.something=This value comes from complex-app's complex2.conf
The setting 'simple-lib.foo' is: This value comes from complex-app's complex2.conf in its custom simple-lib-context
The setting 'simple-lib.hello' is: This value comes from simple-lib's reference.conf
The setting 'simple-lib.whatever' is: This value comes from a system property
when we passed a bad config to simple-lib, it said: complex2.conf @ jar:file:/var/folders/5l/vlthy3b54cg70yvnrtvxygf00000gn/T/sbt_cbd750d3/job-2/target/5b0446ca/config-complex-app-scala_2.10-1.3.0-1fc2aff45226dfc863ab8a0e54c0ae946b2b30c8.jar!/complex2.conf: 8: simple-lib.hello: No setting at 'simple-lib.hello', expecting: string

*/ 