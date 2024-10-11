# AWS-Jenkins-CICD
 Jenkins 파이프라인을 통해 github push event 발생 시, jar파일 빌드, AWS S3로 업로드 후 Amazon SQS 메세지를 활용하여 ec2가 jar버전 변경을 감지하고, 이를 가져와 실행하는 CICD 과정을 다룬다.

# 👨🏻‍💻👩🏻‍💻 팀원 소개

| 이승준 | 이주원 |
|:-----------:|:-----------:|
| <img width="120px" src="https://avatars.githubusercontent.com/leesj000603"/> | <img width="120px" src="https://avatars.githubusercontent.com/2oo1s"/> |
| [@leesj000603](https://github.com/leesj000603) | [@2oo1s](https://github.com/2oo1s) |


# 실습 과정
### Amazon rds에 연결한 간단한 기사를 작성하여 저장하는 프로젝트 생성 / github에 등록
![image](https://github.com/user-attachments/assets/a0b4241d-8597-40ba-a8b8-7e1ded98ebe1)

![image](https://github.com/user-attachments/assets/71c5e5c9-b4eb-41e2-9e39-d677011555d6)

![image](https://github.com/user-attachments/assets/35ba6fe3-4f34-4505-83e6-6291ace612a7)


## docker 내 jenkins 설치 및 ngrok 설정

```bash
# docker 설치
sudo apt install docker.io

# jenkins 설치 및 실행
docker run --name myjenkins --privileged -p 8888:8080
 -v $(pwd)/appjardir:/var/jenkins_home/appjar jenkins/jenkins:lts-jdk17
```

ngrok 설정하기

```bash
ngrok http http://127.0.0.1:8888
```

![image](https://github.com/user-attachments/assets/06ea8739-2ffe-4cff-89b5-524fafa0caab)

## github webhook 설정
![image](https://github.com/user-attachments/assets/62649437-38f7-4586-bb51-681fd336e0d7)
![image](https://github.com/user-attachments/assets/45249f10-2b28-4892-afee-b9812cc7d7b5)
![image](https://github.com/user-attachments/assets/38eb7c1d-71df-438f-8059-114f4afab8ee)

### 
```bash
sudo apt install awscli

# s3로 jar 파일 복사
cp /home/username/appjardir/myApp-0.0.1-SNAPSHOT.jar s3://ce2228-bucket-01/myapp.jar
```

## S3 버킷 생성
![image](https://github.com/user-attachments/assets/f3f8a5e4-1eed-4270-889f-e4c2c2e9ee61)


## jenkins 파이프라인 및 기타 설정

파이프라인 구성 script

```shell
pipeline {
    agent any

    stages {
        stage('Clone Repository') {
            steps {
                git branch: 'cicd', url: 'https://github.com/leesj000603/AWS-Jenkins-CICD.git'
            }
        }
        
        stage('Build') {
            steps {
                dir('./') {                   
                    sh 'chmod +x gradlew'                    
                    sh './gradlew clean build -x test'
                    sh 'echo $WORKSPACE'   
                }
            }
        }
        
        stage('Copy jar') { 
            steps {
                script {
                    def jarFile = '/var/jenkins_home/workspace/aws/build/libs/myApp-0.0.1-SNAPSHOT.jar'                   
                    sh "cp ${jarFile} /var/jenkins_home/appjar/"
                }
            }
        }
        
        stage('Copy to s3') {
            steps {
                script {
                    def host = 'username@10.0.2.30' // 호스트의 사용자 이름과 IP 주소
                    sshagent(['myjenkins']) {
                        // 스크립트 실행
                        sh "ssh -o StrictHostKeyChecking=no ${host} 'aws s3 cp /home/username/appjardir/myApp-0.0.1-SNAPSHOT.jar s3://ce2228-bucket-01/myapp.jar'"
                    }
                }
            }
        }
    }
}
```

jenkins Credentials에 로컬과 소통할 ssh private key 등록

![image](https://github.com/user-attachments/assets/360a711e-c7ba-44be-92e7-2e961024b9f6)

## Amazon Simple Queue Service 생성
### 세부 정보 설정
![image](https://github.com/user-attachments/assets/e7316091-535b-4857-adf9-c25677ac09f2)
### 액세스 정책 설정
```
{
  "Version": "2012-10-17",
  "Id": "PolicyForS3",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "s3.amazonaws.com"
      },
      "Action": "SQS:SendMessage",
      "Resource": "arn:aws:sqs:region:account-id:queue-name",
      "Condition": {
        "ArnLike": {
          "aws:SourceArn": "arn:aws:s3:::your-bucket-name"
        }
      }
    }
  ]
}
```


## S3 SQS 메세지 발송 설정
### s3 → 버킷 → 속성

### put과 post에 대하여 이벤트 알림을 sqs로 보내도록 설정
![image](https://github.com/user-attachments/assets/9c8e19d1-6cba-46c2-8948-ac554389ba94)
### 생성했던 sqs대기열 선택
![image](https://github.com/user-attachments/assets/da43ec0a-4f1b-4460-859f-202fa49294dc)
ㅇㅇ
