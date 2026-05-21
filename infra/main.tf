# main.tf

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

# 서울 리전 설정
provider "aws" {
  region = "ap-northeast-2"
}

# 1. ECR 리포지토리 생성 (도커 이미지 창고)
resource "aws_ecr_repository" "token_ledger_repo" {
  # 주의: 깃허브 액션 deploy.yml에 적혀있는 ECR 이름과 정확히 똑같이 맞춰주세요!
  name                 = "token-ledger-app"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# 2. ECS 클러스터 생성 (컨테이너들이 뛰어놀 공간)
resource "aws_ecs_cluster" "token_ledger_cluster" {
  name = "token-ledger-cluster"
}