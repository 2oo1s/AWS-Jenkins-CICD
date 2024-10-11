# AWS-Jenkins-CICD
 Jenkins 파이프라인을 통해 github push event 발생 시, jar파일 빌드, AWS S3로 업로드 후 Amazon SQS 메세지를 활용하여 ec2가 jar버전 변경을 감지하고, 이를 가져와 실행하는 CICD 과정을 다룬다.

## 👨🏻‍💻👩🏻‍💻 팀원 소개

| 이승준 | 이주원 |
|:-----------:|:-----------:|
| <img width="120px" src="https://avatars.githubusercontent.com/leesj000603"/> | <img width="120px" src="https://avatars.githubusercontent.com/2oo1s"/> |
| [@leesj000603](https://github.com/leesj000603) | [@2oo1s](https://github.com/2oo1s) |


## 실습 과정
### Amazon rds에 연결한 간단한 기사를 작성하여 저장하는 프로젝트 생성 / github에 등록
![image](https://github.com/user-attachments/assets/a0b4241d-8597-40ba-a8b8-7e1ded98ebe1)

![image](https://github.com/user-attachments/assets/71c5e5c9-b4eb-41e2-9e39-d677011555d6)

![image](https://github.com/user-attachments/assets/35ba6fe3-4f34-4505-83e6-6291ace612a7)


### jenkins ngrok


### github webhook 설정
![image](https://github.com/user-attachments/assets/62649437-38f7-4586-bb51-681fd336e0d7)
![image](https://github.com/user-attachments/assets/45249f10-2b28-4892-afee-b9812cc7d7b5)
![image](https://github.com/user-attachments/assets/38eb7c1d-71df-438f-8059-114f4afab8ee)


### S3 버킷 생성
![image](https://github.com/user-attachments/assets/f3f8a5e4-1eed-4270-889f-e4c2c2e9ee61)


### jenkins 파이프라인 및 기타 설정





### Amazon 
