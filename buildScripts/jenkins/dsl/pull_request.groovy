def NAME = "kong"
def EMAIL = "_devmicrocosm@kenshoo.com"
def JOB_NAME = "${NAME}-pull-request"

job(JOB_NAME) {
    label("microcosm-centos7-ecr")

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
        }
    }

    triggers {
        githubPullRequest {
            orgWhitelist('Kenshoo')
            useGitHubHooks()
        }
    }

    steps {
        shell("""
          virtualenv -p python3.7 venv
          source venv/bin/activate
          pip install -r requirements.txt --extra-index-url="https://\${MICROSERVICES_ARTIFACTORY_USER}:\${MICROSERVICES_ARTIFACTORY_PASSWORD}@artifactory.kenshoo-lab.com/artifactory/api/pypi/PyPI-releaes/simple/" --trusted-host artifactory.kenshoo-lab.com
          sudo yum install lua-devel-5.1.4-15.el7.x86_64 -y
          wget https://luarocks.org/releases/luarocks-3.7.0.tar.gz --no-check-certificate
          tar zxpf luarocks-3.7.0.tar.gz
          cd luarocks-3.7.0
          ./configure && make && sudo make install
          sudo luarocks install luasocket
          cd ..
          invoke clean test
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