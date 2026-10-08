variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-south-1"
}

variable "ecr_repository_name" {
  description = "ECR repository name"
  type        = string
  default     = "springboot-mysql-thymeleaf-devsecops"
}