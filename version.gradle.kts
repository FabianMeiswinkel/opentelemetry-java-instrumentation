val stableVersion = "1.27.1-SuppressInstrumentationOnAssembly"
val alphaVersion = "1.27.1-SuppressInstrumentationOnAssembly-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
