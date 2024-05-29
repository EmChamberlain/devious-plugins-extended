version = "0.0.4"

project.extra["PluginName"] = "Unethical GOTR"
project.extra["PluginDescription"] = "GOTR"

dependencies {
    implementation("com.datbear:library:1.1")
}
tasks {
    jar {
        manifest {
            attributes(mapOf(
                    "Plugin-Version" to project.version,
                    "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                    "Plugin-Provider" to project.extra["PluginProvider"],
                    "Plugin-Description" to project.extra["PluginDescription"],
                    "Plugin-License" to project.extra["PluginLicense"]
            ))
        }
    }
}
