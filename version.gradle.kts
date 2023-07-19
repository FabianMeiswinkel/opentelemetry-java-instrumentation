val stableVersion = "1.27.1-SuppressInstrumentationAlways"
val alphaVersion = "1.27.1-SuppressInstrumentationAlways-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
