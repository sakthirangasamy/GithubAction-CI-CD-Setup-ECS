resource "aws_ecr_repository" "app" {
  name                 = var.ecr_repository_name
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  force_delete = true

  tags = {
    Project     = "GithubAction-CI-CD-Setup-ECS"
    Environment = "dev"
    ManagedBy   = "Terraform"
  }
}