val stableVersion = "1.27.1-SuppressInstrumentationViaDelegatingScannable"
val alphaVersion = "1.27.1-SuppressInstrumentationViaDelegatingScannable-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
