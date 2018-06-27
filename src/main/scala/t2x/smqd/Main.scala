package t2x.smqd

import java.io.{File, InputStreamReader}

import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
  * 2018. 5. 29. - Created by Kwon, Yeong Eon
  */
trait SmqMain extends App with StrictLogging {
  def dumpEnvNames: Seq[String] = Nil

  // override for rebranding logo and application's version
  def logo: String = ""
  def versionString = ""
  def configBasename = "smqd"

  def buildSmqd(): Smqd = {
    val config = buildConfig()
    // print out logo first then load config,
    // because there is chances to fail while loading config,
    if (config == null) {
      logger.info(logo+"\n")
      scala.sys.exit(-1)
    }
    else {
      val smqdVersion: String = config.getString("smqd.version")
      logger.info(s"$logo $versionString(smqd: $smqdVersion)\n")
    }

    //// for debug purpose /////
    dumpEnvNames.foreach{ k =>
      val v = System.getProperty(k, "<not defined>")
      logger.trace(s"-D$k = $v")
    }
    //// build configuration ///////

    try {
      SmqdBuilder(config).build()
    }
    catch {
      case ex: Throwable =>
        logger.error("initialization failed", ex)
        System.exit(1)
        null
    }
  }

  // override this to change the way of loading config
  def buildConfig(): Config = {
    val akkaRefConfig = ConfigFactory.load( "reference.conf")
    val smqdRefConfig = ConfigFactory.load("smqd-ref.conf")

    val configFilePath = System.getProperty("config.file")
    val configResourcePath = System.getProperty("config.resource")

    if (configFilePath != null) {
      val configFile = new File(configFilePath)

      if (!configFile.exists) {
        logger.error(s"config.file=$configFilePath not found.")
        scala.sys.error(s"config.file=$configFilePath not found.")
        return null
      }

      ConfigFactory.parseFile(configFile).withFallback(smqdRefConfig).withFallback(akkaRefConfig).resolve()
    }
    else if (configResourcePath != null) {
      val in = getClass.getClassLoader.getResourceAsStream(configResourcePath)
      if (in == null) {
        logger.error(s"config.resource=$configResourcePath not found.")
        scala.sys.error(s"config.resource=$configFilePath not found.")
        return null
      }
      val configReader = new InputStreamReader(in)
      ConfigFactory.parseReader(configReader).withFallback(smqdRefConfig).withFallback(akkaRefConfig).resolve()
    }
    else {
      ConfigFactory.load(configBasename).withFallback(smqdRefConfig).withFallback(akkaRefConfig).resolve()
    }
  }
}

object Main extends SmqMain with StrictLogging {

  override val dumpEnvNames = Seq(
    "config.file",
    "logback.configurationFile",
    "java.net.preferIPv4Stack",
    "java.net.preferIPv6Addresses"
  )

  override val logo: String =
    """
      | ____   __  __   ___   ____
      |/ ___| |  \/  | / _ \ |  _ \
      |\___ \ | |\/| || | | || | | |
      | ___) || |  | || |_| || |_| |
      ||____/ |_|  |_| \__\_\|____/ """.stripMargin

  // override for rebranding
  override val versionString: String = ""

  try{
    val smqd = super.buildSmqd()
    smqd.start()
  }
  catch {
    case ex: Throwable =>
      logger.error("starting failed", ex)
      System.exit(1)
  }
}