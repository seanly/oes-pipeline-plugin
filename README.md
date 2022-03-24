# oes-pipeline plugin

一种使用ant扩展jenkins构建能力的插件

# 调试方法


# 配置语法

1. 样例一

```yaml
environment:
  JENKINS: jenkins

pipeline:
  - name: build
    steps:
      - script: 
          code: |
            echo "hello, $JENKINS"
```

2. 样例二

特点：少了一层缩进
```yaml
environment:
  JENKINS: jenkins

pipeline:
  - name: build
    steps:
      - step.id: script 
        code: |
          echo "hello, $JENKINS"
```

3. 样例三

```yaml
environment:
  DOCKER_REG: registry.cn-hangzhou.aliyuncs.com
  DOCKER_AUTH: secret://jenkins/usernamePassword/aliyun-acr-chengdu
  ASL_IMG_PREFIX: registry.cn-hangzhou.aliyuncs.com/k8ops/jenkins-build
  ASL_IMG_KUBECTL: ${ASL_IMG_PREFIX}:kubetool
  ASL_IMG_MAVEN: ${ASL_IMG_PREFIX}:maven-java8u201
  ASL_IMG_NODE: ${ASL_IMG_PREFIX}:node-12184
  REG_PREFIX: registry.cn-hangzhou.aliyuncs.com/k8ops-apps
  KUBECONFIG: secret://jenkins/secretFile/kubeconfig-xxx-k8s-online
  APP_GROUP: k8ops
  APP_NAME: java-docker-sample
  APP_IMG: ${REG_PREFIX}/${APP_NAME}:${APP_VERSION}

pipeline:
  - name: build
    steps:
      - dapper:
          template.id: maven-java8u201
      - step.id: script 
        code: |
          echo "--//INFO: list target directory files"
          ls -l ./target/
      - step.id: docker
        command: build
        build.tag: ${APP_IMG}
        workdir: .
        dockerfile: .dapper/Dockerfile
        docker.opts: --pull
  - name: deploy
    steps:
      - step.id: kubectl
        toolenv.img: ${ASL_IMG_KUBECTL} 
        command: set image
        workload: deployment/${env.APP_NAME}
        container: ${env.APP_NAME}
        image: ${env.APP_IMG}
        namespace: ${env.APP_GROUP}-${env.APP_ENV}
```

# Jenkins Pipeline语法

```groovy

pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo '--//INFO: this is a test pipeline'
                oesStep stepId: "sample", 
                    stepProps: [
                        stepProp(key: "arg1", value:"hi, jenkins oes-step")
                    ]
                    
                oesPipeline environs: """
                        HELLO_v1=hi, oes-pipeline
                        _RUN_STAGES=build
                    """.stripIndent(),
                    provider: oesPipelineConfigFromJenkins(content: '''
                        environment:
                          HELLO: ${HELLO_v1}
                        
                        pipeline:
                          - name: build
                            steps:
                              - step.id: sample 
                                arg1: 'hi, oes-jenkins'
                    '''.stripIndent()
                    )
            }
        }
    }
}

```

# gitlab 作为step registry

需要提供默认step/asl库地址

[TODO]
