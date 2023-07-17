val stableVersion = "1.27.1-SuppressInstrumentationViaContext"
val alphaVersion = "1.27.1-SuppressInstrumentationViaContext-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
