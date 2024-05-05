def NAME = "memsql-base-automation-image-builder"
def EMAIL = "shmulik.gutman@skai.io,shaked.nahum@skai.io"
def JOB_NAME = "${NAME}-pull-request"

job(JOB_NAME) {
    label("microcosm-ubuntu-base")

    logRotator(10,10)
    concurrentBuild(true)

    throttleConcurrentBuilds{
        maxPerNode 1
        maxTotal 10
    }

    scm {
        git {
            remote {
                url("https://github.com/kenshoo/${NAME}.git")
                credentials('jenkins-microcosm-github-app')
                refspec('+refs/pull/*:refs/remotes/origin/pr/*')
            }

            configure { node ->
                node / 'extensions' / 'hudson.plugins.git.extensions.impl.CleanBeforeCheckout' {}
            }

            branch("\${sha1}")
        }
    }

    configure { project ->
        def properties = project / 'properties'
        properties<< {
            'com.coravy.hudson.plugins.github.GithubProjectProperty'{
                projectUrl "https://github.com/kenshoo/${NAME}/"
            }
        }
    }

    wrappers {
        preBuildCleanup()
        timestamps()
        injectPasswords()
        colorizeOutput()
        timeout {
            absolute(120)
        }
        sshAgent('kgithub-build-jenkins-microcosm-key')
        credentialsBinding {
            usernamePassword('MICROSERVICES_ARTIFACTORY_USER', 'MICROSERVICES_ARTIFACTORY_PASSWORD', 'jcasc_deployer-microcosm')
            string('DOCKERHUB_TOKEN_ENV', 'DOCKERHUB_TOKEN')
            string('DOCKER_ARTIFACTORY_TOKEN_ENV', 'DOCKER_ARTIFACTORY_TOKEN')
        }
    }

    triggers {
        githubPullRequest {
            orgWhitelist('Kenshoo')
            useGitHubHooks()
        }
    }

    steps {
        configFileBuildStep {
            managedFiles {
                configFile {
                    fileId('pip-conf-microcosm')
                    replaceTokens(true)
                    targetLocation("/home/ubuntu/.config/pip/pip.conf")
                }
                configFile {
                    fileId('microcosm-docker-config')
                    replaceTokens(true)
                    targetLocation("/home/ubuntu/.docker/config.json")
                }
            }
        }
    }

    steps {
        shell("""
          make
          docker build -t 668139184987.dkr.ecr.us-east-1.amazonaws.com/ks-db-memsql-cluster-in-a-box-base:8.1.31 -f Dockerfile-ciab . && echo "Successfully built base image"
      """)
    }

    publishers {

        extendedEmail {
            recipientList("${EMAIL}")
            triggers {
                unstable {
                    sendTo {
                        requester()
                        developers()
                    }
                }
                failure {
                    sendTo {
                        requester()
                        developers()
                    }
                }
                statusChanged {
                    sendTo {
                        requester()
                        developers()
                    }
                }

                configure { node ->
                    node / contentType << 'text/html'
                }
            }
        }

    }
}