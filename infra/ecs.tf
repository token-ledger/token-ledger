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

# 3. 선수 명단 (Task Definition): "어떤 이미지를, 얼마만큼의 사양으로 띄울까?"
resource "aws_ecs_task_definition" "app_task" {
  family                   = "token-ledger-app-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256" # 0.25 vCPU (프리티어 가성비)
  memory                   = "512" # 0.5 GB RAM
  execution_role_arn       = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name      = "token-ledger-container"
      # 테라폼이 알아서 아까 만든 ECR 창고 주소를 가져오고, 깃허브가 밀어넣은 latest 태그를 붙입니다!
      image     = "${aws_ecr_repository.token_ledger_repo.repository_url}:latest"
      essential = true
      portMappings = [
        {
          containerPort = 8080
          hostPort      = 8080
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.ecs_log_group.name
          "awslogs-region"        = "ap-northeast-2"
          "awslogs-stream-prefix" = "ecs"
        }
      }
    }
  ])
}

# 4. 실제 경기 출전 (ECS Service): "선수 명단대로 진짜 서버를 실행해라!"
resource "aws_ecs_service" "app_service" {
  name            = "token-ledger-test-app-task-service" # 깃허브 액션에 적어둔 이름과 똑같이 맞춤!
  cluster         = aws_ecs_cluster.token_ledger_cluster.id
  task_definition = aws_ecs_task_definition.app_task.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.public_a.id, aws_subnet.public_c.id]
    security_groups  = [aws_security_group.ecs_sg.id]
    assign_public_ip = true # 우리가 인터넷 창에서 접속해야 하므로 퍼블릭 IP 열어주기
  }
}