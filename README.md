# AWS-Jenkins-CICD
Jenkins 파이프라인을 통해 github push event 발생 시, jar 파일을 빌드하고 AWS S3로 업로드 후, Amazon SQS 메세지를 활용하여 ec2가 jar 버전 변경을 감지하고 이를 가져와 실행하는 CICD 과정을 다룬다.

## Architecture
![image](https://github.com/user-attachments/assets/92b7539c-e39e-432b-8c82-a2709d347a6f)



## 👨🏻‍💻👩🏻‍💻 팀원 소개

| 이승준 | 이주원 |
|:-----------:|:-----------:|
| <img width="120px" src="https://avatars.githubusercontent.com/leesj000603"/> | <img width="120px" src="https://avatars.githubusercontent.com/2oo1s"/> |
| [@leesj000603](https://github.com/leesj000603) | [@2oo1s](https://github.com/2oo1s) |


## 실습 과정
### 1. Amazon rds에 연결한 간단한 기사를 작성하고 저장하는 프로젝트 생성 및 github에 업로드

![image](https://github.com/user-attachments/assets/a0b4241d-8597-40ba-a8b8-7e1ded98ebe1)

![image](https://github.com/user-attachments/assets/71c5e5c9-b4eb-41e2-9e39-d677011555d6)

![image](https://github.com/user-attachments/assets/35ba6fe3-4f34-4505-83e6-6291ace612a7)


### 2. Docker 내 Jenkins 설치 및 ngrok 설정

호스트의 디렉토리`($(pwd)/appjardir)`를 컨테이너의 `/var/jenkins_home/appjar` 경로에 마운트하여 데이터가 지속적으로 유지되도록 설정한다.

```bash
# docker 설치
sudo apt install docker.io

# 권한 추가 및 적용
$sudo usermod -a -G docker $USER 
$newgrp docker    
$groups

# jenkins 설치 및 실행
docker run --name myjenkins --privileged -p 8888:8080 \
 -v $(pwd)/appjardir:/var/jenkins_home/appjar jenkins/jenkins:lts-jdk17
```

ngrok 설정하기

```bash
ngrok http http://127.0.0.1:8888
```

![image](https://github.com/user-attachments/assets/06ea8739-2ffe-4cff-89b5-524fafa0caab)

ngrok을 통해 생성된 url을 github webhook으로 설정

![image](https://github.com/user-attachments/assets/62649437-38f7-4586-bb51-681fd336e0d7)

#### 
```bash
sudo apt install awscli

# s3로 jar 파일 복사
cp /home/username/appjardir/myApp-0.0.1-SNAPSHOT.jar s3://ce2228-bucket-01/myapp.jar
```

### 3. Jenkins 파이프라인 및 기타 설정

파이프라인 스크립트를 통해 GitHub에서 코드를 클론하고, Gradle로 빌드한 후, 생성된 JAR 파일을 특정 디렉토리로 복사하고, 마지막으로 AWS S3 버킷에 업로드하는 과정을 자동화한다.

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

Jenkins와 호스트 머신의 소통을 위한 ssh key를 설정해준다.

```shell
# myjenkins bash 
username@awsclient:~$ docker exec -u root -it myjenkins bash

# 기본 경로에 키 생성 후, 저장 / 비밀번호 생성 x
root@cd32961bc995:/# ssh-keygen -t rsa -b 4096 -C "your_email@example.com"

# public key 확인 후, 복사
root@cd32961bc995:/# cat ~/.ssh/id_rsa.pub

# host의 ~/.ssh/authorized_keys에 적용
username@awsclient:~$ echo "복사한 키" >> ~/.ssh/authorized_keys

# private 키 복사 후, jenkins credential에 등록
username@awsclient:~$ cat ~/.ssh/id_rsa
```

Jenkins Credentials에 ssh private key를 등록해준다.

![image](https://github.com/user-attachments/assets/360a711e-c7ba-44be-92e7-2e961024b9f6)

### 4. S3 버킷 생성

S3 버킷 이름 설정 및 ACL 비활성화
![image](https://github.com/user-attachments/assets/f3f8a5e4-1eed-4270-889f-e4c2c2e9ee61)

버킷 퍼블릭 액세스 허용
![image](https://github.com/user-attachments/assets/6ce1290d-108c-4e3a-b8c5-96bd31489bc0)

### 5. Amazon Simple Queue Service 생성

#### 세부 정보 설정

![image](https://github.com/user-attachments/assets/e7316091-535b-4857-adf9-c25677ac09f2)

#### 액세스 정책 설정
ARN (Amazon Resource Name) 을 통해 S3버킷에 대한 SQS 메세지 수신을 허용
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


### 6. S3 SQS 메세지 발송 설정
#### s3 → 버킷 → 속성 -> 이벤트 알림 생성
![image](https://github.com/user-attachments/assets/9c8e19d1-6cba-46c2-8948-ac554389ba94)

#### 모든 객체 생성 이벤트에 대하여 알림을 보내도록 설정
![image](https://github.com/user-attachments/assets/4fe35a29-85ec-4468-82a0-ef99a65949c7)



#### 생성했던 sqs대기열에 이벤트 메세지를 보내도록 등록한다.
![image](https://github.com/user-attachments/assets/da43ec0a-4f1b-4460-859f-202fa49294dc)

 
### 7. iam 역할 생성
ec2에서 SQS로부터 메세지를 읽어들일 수 있는 권한과, S3에서 jar파일을 복사해 가져올 수 있는 권한이 필요하다. 
![image](https://github.com/user-attachments/assets/0cb78fed-2e9f-4ad5-9bf8-305f77523c28)

#### ec2에 적용 할 것이므로 aws 서비스, ec2 사용사례 선택
![image](https://github.com/user-attachments/assets/0f1c265d-f170-4c7c-b369-b47c9ef41981)

#### 권한 amazonS3ReadOnly, amazonSQSFullAccess 추가 
![image](https://github.com/user-attachments/assets/cd1222c8-c5d9-4e9f-b10b-1ed30dd3ba92)

![image](https://github.com/user-attachments/assets/fd75cfed-296b-4050-8385-9ab800c40e4b)

#### 역할 이름 지정 및 생성

![image](https://github.com/user-attachments/assets/798efad0-b553-4dec-9603-dce0fb1fdc52)

#### 추가적인 인라인 연결 정책 설정 (메세지 권한)

![image](https://github.com/user-attachments/assets/2d12ff6e-2ac8-4e60-a608-fba07c341354)

```
{
	"Version": "2012-10-17",
	"Statement": [
		{
			"Effect": "Allow",
			"Action": [
				"sqs:ReceiveMessage",
				"sqs:DeleteMessage",
				"sqs:GetQueueAttributes"
			],
			"Resource": "arn:aws:sqs:ap-northeast-2:646580111040:ce2228-jar-update"
		}
	]
}
```

### 8. iam 권한 ec2에 설정

#### ec2 선택

![image](https://github.com/user-attachments/assets/5fd32b4e-83c5-4e64-a28c-946729502088)

#### 작업 → 보안 → iam 역할 수정

![image](https://github.com/user-attachments/assets/b09dba3d-5dc8-49ef-85fc-573e541c860f)

![image](https://github.com/user-attachments/assets/e42de34f-c852-481a-a152-fb91e3ae5285)

이제 ec2는 s3로 부터 날아온 SQS 에 접근이 가능하다.


### S3에서 jar파일이 변경되었을 때의 SQS메세지 수신 및 S3의 jar 복사 및 실행 스크립트
```python
python
# SQS 메시지 수신 및 처리
import os
import boto3
import subprocess
import json
import time

# S3 및 SQS 설정
s3_bucket_name = 'your-s3-bucket-name'  # 예시: 'ce2228-bucket-01'
queue_url = 'https://sqs.ap-northeast-2.amazonaws.com/your-account-id/your-queue-name'  # 예시: 'https://sqs.ap-northeast-2.amazonaws.com/646580111040/ce2228-jar-update'
local_jar_dir = '/path/to/your/local/directory/'  # 예시: '/home/ubuntu/appjardir/'

# S3에서 JAR 파일 다운로드
def download_jar(s3_key):
    s3 = boto3.client('s3')

    # 다운로드할 JAR 파일의 전체 경로 설정
    local_jar_path = os.path.join(local_jar_dir, os.path.basename(s3_key))

    # 디렉터리가 존재하지 않으면 생성
    os.makedirs(local_jar_dir, exist_ok=True)

    # S3에서 JAR 파일 다운로드
    s3.download_file(s3_bucket_name, s3_key, local_jar_path)
    return local_jar_path  # 다운로드한 JAR 파일 경로 반환

# JAR 파일 실행
def run_jar(local_jar_path):
    try:
        # 8080 포트에서 실행 중인 프로세스를 찾고 종료
        result = subprocess.run(['lsof', '-t', '-i:8080'], stdout=subprocess.PIPE, text=True)
        pid = result.stdout.strip()

        if pid:
	    subprocess.run(['kill', '-9', pid])

        # 새로운 JAR 파일 실행
        subprocess.run(['java', '-jar', local_jar_path], check=True)

    except subprocess.CalledProcessError as e:
        print(f"JAR 파일 실행 중 오류 발생: {e}")

# SQS 메시지 수신 및 처리
def process_sqs_messages():
    sqs = boto3.client('sqs', region_name='ap-northeast-2')

    while True:
        try:
            messages = sqs.receive_message(QueueUrl=queue_url)
            if 'Messages' in messages:
                for message in messages['Messages']:
                    message_body = json.loads(message['Body'])
                    print("Received message:", message['Body'])

                    if 'Records' in message_body:
                        s3_key = message_body['Records'][0]['s3']['object']['key']
                        print(f"S3에서 JAR 파일 {s3_key} 다운로드 중...")
                        local_jar_path = download_jar(s3_key)  # JAR 파일 다운로드
                        run_jar(local_jar_path)  # JAR 파일 실행
                        sqs.delete_message(QueueUrl=queue_url, ReceiptHandle=message['ReceiptHandle'])  # 메시지 삭제
                    else:
                        print("메시지에 'Records' 키가 없습니다. 다른 유형의 메시지입니다.")
        except Exception as e:
            print(f"오류 발생: {e}")
        time.sleep(5)  # 메시지가 없을 때 잠시 대기

if __name__ == "__main__":
    process_sqs_messages()
```

#### 1. 라이브러리 및 환경 설정
- boto3: AWS 서비스와 상호작용하기 위한 Python SDK이다. 여기서는 S3와 SQS에 연결한다.
- subprocess: 파이썬 코드에서 외부 프로세스를 실행하기 위한 모듈이다. 여기서는 JAR 파일을 실행한다.
- json: SQS에서 수신한 메시지를 JSON 형식으로 파싱하기 위한 모듈이다.
- time: SQS 메시지 수신 간격을 조정하기 위해 사용한다.

**환경 변수:**

- s3_bucket_name: S3에서 JAR 파일을 다운로드할 버킷 이름을 설정.
- queue_url: SQS 대기열의 URL을 설정합.
- local_jar_dir: 다운로드한 JAR 파일을 저장할 로컬 디렉터리 경로.


#### download_jar함수
- S3 클라이언트 생성: boto3.client('s3')를 통해 S3에 연결한다.
- JAR 파일 경로 설정: S3에서 다운로드할 파일을 저장할 로컬 경로를 생성한다. s3_key는 S3에서 다운로드할 파일의 경로이고, os.path.basename(s3_key)는 파일 이름을 추출한다.
- 디렉터리 생성: 로컬 경로에 디렉터리가 존재하지 않으면 os.makedirs()로 새로 생성한다.
- S3 다운로드: s3.download_file()을 사용해 S3에서 파일을 다운로드하고, 그 파일의 로컬 경로를 반환한다.

#### run_jar 함수
- 포트 충돌을 피하기 위해 8080 포트 확인 후 실행중인 프로세스가 있다면 종료.
- JAR 파일 실행: subprocess.run()을 통해 JAR 파일을 실행한다. 여기서는 java -jar <jar파일> 명령을 사용한다.
- 오류 처리: 만약 프로세스 종료 또는 JAR 파일 실행 도중 문제가 발생하면, subprocess.CalledProcessError 예외를 잡아 에러 메시지를 출력한다.

#### process_sqs_messages 함수
- SQS 클라이언트 생성: boto3.client('sqs')로 SQS에 연결한다. 이 클라이언트를 사용해 메시지를 받아온다.
- 무한 루프: SQS 메시지를 지속적으로 수신하기 위해 while True 루프를 사용한다.
- 메시지 수신: sqs.receive_message()를 통해 SQS 대기열에서 메시지를 가져온다. 수신된 메시지는 리스트 형태로 반환된다.
- 메시지 처리:
  - 메시지 본문은 JSON 형식으로 되어 있으므로 json.loads()로 파싱하여 message_body에 저장한다.
  - 수신된 메시지에 Records가 포함되어 있으면, 이는 S3 이벤트와 연관된 메시지로 간주되며, 그 안에서 S3의 파일 경로(= s3_key)를 가져온다.
  - JAR 파일을 다운로드하고 실행한 후, 메시지 처리가 완료되면 sqs.delete_message()로 SQS에서 해당 메시지를 삭제한다.
- 오류 처리: 메시지 수신 중 발생하는 예외는 try-except 블록으로 처리하여 에러가 발생할 때마다 이를 출력한다.
- 대기 시간: 메시지가 없을 때에는 5초 대기 후 다시 메시지를 확인하는 방식으로 리소스 소모를 줄인다.

#### 스크립트 실행 후 jar파일이 저장되는 S3의 이벤트를 읽어들여 jar파일을 가져와 실행하는 모습
![image](https://github.com/user-attachments/assets/06856220-c908-4874-8f02-4e3dc478d27d)

#### 실행 중인 모습
간단한 내용을 삽입 - 생성했던 RDS에 정상적으로 저장되어야 정상 동작이다.
![image](https://github.com/user-attachments/assets/f4a1dce6-49b6-400d-983a-b653e474e366)

#### RDS에도 저장됨을 DBeaver로 확인
RDS를 DBeaver 연결 해놓은 connection
![image](https://github.com/user-attachments/assets/c34bd458-0e03-4bde-a990-9aadcea89b32)
이를 통해 db에 삽입됨을 확인
<br>
![image](https://github.com/user-attachments/assets/bda8d642-f93c-4afc-ac08-b3f266e9e512)

#### SQS 메세지 예시 - ObjectCreated 이벤트가 발생한 것에 대한 메세지를 보내고 있다.

```json
{
  "Records": [
    {
      "eventVersion": "2.1",
      "eventSource": "aws:s3",
      "awsRegion": "ap-northeast-2",
      "eventTime": "2024-10-11T08:12:08.923Z",
      "eventName": "ObjectCreated:CompleteMultipartUpload",
      "userIdentity": {
        "principalId": "AWS:AIDAZNCZNQ3ANTK5GPK2U"
      },
      "requestParameters": {
        "sourceIPAddress": "118.131.63.236"
      },
      "responseElements": {
        "x-amz-request-id": "2PWWHBHRJEJ5C0EJ",
        "x-amz-id-2": "w1GTcVxgv5yO990lBXDwkHU/iXB7JW8Lq/M+O+eIrUTiHOlWRZgOJTZqDS/y+uJa/7ZixdMVhPsZD3k5WmukYY941UwKons2QTHkFCwfrdM="
      },
      "s3": {
        "s3SchemaVersion": "1.0",
        "configurationId": "ce2228-jar-update",
        "bucket": {
          "name": "ce2228-bucket-01",
          "ownerIdentity": {
            "principalId": "A2DGQGKB0NHMNT"
          },
          "arn": "arn:aws:s3:::ce2228-bucket-01"
        },
        "object": {
          "key": "myapp.jar",
          "size": 48924480,
          "eTag": "f438baaa1e4d6b8daff5310a8363d387-6",
          "sequencer": "006708DDC8736214C7"
        }
      }
    }
  ]
}
```



