# ecs.tf

# 1. 권한 (IAM): ECS가 ECR 창고에서 도커 이미지를 꺼내올 수 있는 '출입증'
resource "aws_iam_role" "ecs_task_execution_role" {
  name = "token-ledger-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = { Service = "ecs-tasks.amazonaws.com" }
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role       = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# 2. 로그 (CloudWatch): 스프링 부트 콘솔 로그를 저장할 공간
resource "aws_cloudwatch_log_group" "ecs_log_group" {
  name              = "/ecs/token-ledger-app"
  retention_in_days = 7
}

# 3. 선수 명단 (Task Definition): 스프링 부트 + 그라파나
resource "aws_ecs_task_definition" "app_task" {
  family                   = "token-ledger-app-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]

  # 💡 3개의 컨테이너를 위해 체급을 한 단계 더 올립니다. (프리티어 이내 안전권)
  cpu                      = "512"  # 0.5 vCPU
  memory                   = "2048" # 2 GB RAM
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    # 🏃‍♂️ 1번 선수: 스프링 부트
    {
      name      = "token-ledger-container"
      image     = "${aws_ecr_repository.token_ledger_repo.repository_url}:latest"
      essential = true
      portMappings = [{ containerPort = 8080, hostPort = 8080, protocol = "tcp" }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
          "awslogs-region"        = "ap-northeast-2"
          "awslogs-stream-prefix" = "ecs-spring"
        }
      }
    },

    # 🎨 2번 선수: 그라파나
    {
      name      = "token-ledger-grafana-container"
      image     = "${aws_ecr_repository.token_ledger_grafana_repo.repository_url}:latest"
      # AWS 환경에서는 localhost로 묶어줍니다!
      environment = [
        {
          name  = "PROMETHEUS_URL"
          value = "http://localhost:9090"
        },
        {
          name  = "GF_AUTH_ANONYMOUS_ENABLED"
          value = "true"
        },
        {
          name  = "GF_AUTH_ANONYMOUS_ORG_NAME"
          value = "Main Org."
        },
        # 👇 AWS 테라폼 코드에도 이 두 줄을 추가해 줍니다.
        {
          name  = "GF_AUTH_ANONYMOUS_ORG_ROLE"
          value = "Viewer"
        },
        {
          name  = "GF_AUTH_DISABLE_LOGIN_FORM"
          value = "false"
        }
      ]
      essential = true
      portMappings = [{ containerPort = 3000, hostPort = 3000, protocol = "tcp" }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
          "awslogs-region"        = "ap-northeast-2"
          "awslogs-stream-prefix" = "ecs-grafana"
        }
      }
    },

    # 🕵️‍♂️ [추가] 3번 선수: 프로메테우스 (외부 포트는 막고 내부 9090만 활성화)
    {
      name      = "token-ledger-prometheus-container"
      image     = "${aws_ecr_repository.token_ledger_prometheus_repo.repository_url}:latest"
      essential = true
      portMappings = [{ containerPort = 9090, hostPort = 9090, protocol = "tcp" }]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
          "awslogs-region"        = "ap-northeast-2"
          "awslogs-stream-prefix" = "ecs-prometheus"
        }
      }
    }
  ])
}

# 4. 실제 경기 출전 (ECS Service): "선수 명단대로 진짜 서버를 실행해라!"
resource "aws_ecs_service" "app_service" {
  name            = "token-ledger-test-app-task-service"
  cluster         = aws_ecs_cluster.token_ledger_cluster.id
  task_definition = aws_ecs_task_definition.app_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  # 💡 [여기 추가] ECS 서비스에 두 개의 타겟 그룹을 각각 매핑합니다.
  load_balancer {
    target_group_arn = aws_lb_target_group.spring_tg.arn
    container_name   = "token-ledger-container" # 컨테이너 이름과 똑같아야 함
    container_port   = 8080
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.grafana_tg.arn
    container_name   = "token-ledger-grafana-container" # 컨테이너 이름과 똑같아야 함
    container_port   = 3000
  }

  network_configuration {
    subnets          = [aws_subnet.public_a.id, aws_subnet.public_c.id]
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = true
  }

  # 타겟 그룹과의 연결 생성을 안정적으로 기다리기 위한 디펜시즈 설정
  depends_on = [aws_lb_listener.http, aws_lb_listener.grafana]
}