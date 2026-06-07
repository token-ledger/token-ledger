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

# 3. 그라파나용 ECR 창고 추가
resource "aws_ecr_repository" "token_ledger_grafana_repo" {
  name                 = "token-ledger-grafana" # 깃허브 액션과 이름이 일치해야 합니다.
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# 4. 프로메테우스용 ECR 창고 추가
resource "aws_ecr_repository" "token_ledger_prometheus_repo" {
  name                 = "token-ledger-prometheus" # 깃허브 액션 환경변수와 일치시킵니다.
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }
}

# ---------------------------------------------------
# [ECR 수명 주기 정책] 각 창고별로 최근 10개의 이미지만 유지하고 나머지는 자동 삭제
# ---------------------------------------------------

# 1. 앱 본체 ECR 정책
resource "aws_ecr_lifecycle_policy" "token_ledger_app_policy" {
  repository = aws_ecr_repository.token_ledger_repo.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}

# 2. 그라파나 ECR 정책
resource "aws_ecr_lifecycle_policy" "token_ledger_grafana_policy" {
  repository = aws_ecr_repository.token_ledger_grafana_repo.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}

# 3. 프로메테우스 ECR 정책
resource "aws_ecr_lifecycle_policy" "token_ledger_prometheus_policy" {
  repository = aws_ecr_repository.token_ledger_prometheus_repo.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 10 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 10
      }
      action = {
        type = "expire"
      }
    }]
  })
}