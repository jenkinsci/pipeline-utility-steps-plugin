def coreJdk11Version="2.161"

def configurations = [
        [ platform: "linux", jdk: "8", jenkins: null ],
        [ platform: "windows", jdk: "8", jenkins: null ],
        [ platform: "linux", jdk: "8", jenkins: coreJdk11Version, javaLevel: "8" ],
        [ platform: "windows", jdk: "8", jenkins: coreJdk11Version, javaLevel: "8" ]
]

if (env.CHANGE_AUTHOR != null && (
                env.CHANGE_AUTHOR == 'batmat' ||
                env.CHANGE_AUTHOR =='jglick' ||
                env.CHANGE_AUTHOR == 'alecharp' ||
                env.CHANGE_AUTHOR == 'oleg-nenashev')) {
    configurations << [ platform: "linux", jdk: "11", jenkins: coreJdk11Version, javaLevel: "8" ]
    configurations << [ platform: "windows", jdk: "11", jenkins: coreJdk11Version, javaLevel: "8" ]
}

buildPlugin(
    findbugs: [run: true, archive:true, unstableTotalAll: "0"],
    configurations: configurations
)
