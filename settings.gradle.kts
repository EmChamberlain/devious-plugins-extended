/*
 * Copyright (c) 2019 Owain van Brakel <https:github.com/Owain94>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

rootProject.name = "unethicalite-plugins"

include("unethical-fighter")
include("unethical-butler")
include("unethical-birdhouses")
include("unethical-kebab-buyer")
include("unethical-autologin")
include("unethical-oneclick")
include("unethical-agility")
include("unethical-prayer")
include("unethical-explorer")
include("unethical-chopper")
include("unethical-zulrah")
include("unethical-cooker")
include("unethical-bankpin")
include("unethical-tempoross")
include("unethical-pickpocket")
include("unethical-logout")

include("m-autoswitcher")
include("m-powerfisher")
include("m-wintertodt")

include("unethical-flicker")
include("unethical-quickflicker")
include("unethical-pestcontrol")
include("unethical-autonightmarezone")
include("unethical-armouranimator")
include("unethical-overheadswitcher")
include("unethical-quicktoggler")
include("unethical-fisher")
include("unethical-actioner")
include("unethical-motherlode")
include("unethical-blastfurnace")
include("unethical-dodgegraphics")
include("unethical-dragonfarmer")
include("unethical-tithefarm")
include("unethical-gotr")
include("unethical-roguesden")
//include("unethical-buylooper")
//include("example-kotlin")

for (project in rootProject.children) {
    project.apply {
        projectDir = file(name)
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${project.path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${project.path} must have a $buildFile build script" }
    }
}
/*
include("unethical-armouranimator:unethical-vorkath")
findProject(":unethical-armouranimator:unethical-vorkath")?.name = "unethical-vorkath"
include("unethical-vorkath")
*/
