import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register

class BootstrapPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register<BootstrapTask>("bootstrapPlugins") {
            if (project == project.rootProject) {
                project.subprojects.forEach {
                    dependsOn(it.project.tasks.get("jar"))
                    mustRunAfter(it.project.tasks.get("jar"))
                }
            }
        }
    }
}
