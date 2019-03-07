def coreJdk11Version="2.164"

def configurations = [
        [ platform: "linux", jdk: "8", jenkins: null ],
        [ platform: "windows", jdk: "8", jenkins: null ],
        [ platform: "linux", jdk: "8", jenkins: coreJdk11Version, javaLevel: "8" ],
        [ platform: "windows", jdk: "8", jenkins: coreJdk11Version, javaLevel: "8" ],
        [ platform: "linux", jdk: "11", jenkins: coreJdk11Version, javaLevel: "8" ],
        [ platform: "windows", jdk: "11", jenkins: coreJdk11Version, javaLevel: "8" ]
]

buildPlugin(
    findbugs: [run: true, archive:true, unstableTotalAll: "0"],
    configurations: configurations
)
