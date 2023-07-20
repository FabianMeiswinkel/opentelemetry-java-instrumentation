val stableVersion = "1.27.1-OnlyPropagateForDifferentContext"
val alphaVersion = "1.27.1-OnlyPropagateForDifferentContext-alpha"

allprojects {
  if (findProperty("otel.stable") != "true") {
    version = alphaVersion
  } else {
    version = stableVersion
  }
}
