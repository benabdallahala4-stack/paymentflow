# Inputs for the illustrative PaymentFlow AWS module. No secrets here — see
# infrastructure/terraform/README.md: this module is never applied.

variable "aws_region" {
  description = "AWS region for the illustrative deployment."
  type        = string
  default     = "eu-west-1"
}

variable "project_name" {
  description = "Name prefix applied to all resources."
  type        = string
  default     = "paymentflow"
}

variable "environment" {
  description = "Environment name (e.g. staging, production). Documentation only."
  type        = string
  default     = "staging"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (ALB)."
  type        = list(string)
  default     = ["10.0.0.0/24", "10.0.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "CIDR blocks for private subnets (ECS tasks, RDS, ElastiCache)."
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "availability_zones" {
  description = "AZs to spread subnets across."
  type        = list(string)
  default     = ["eu-west-1a", "eu-west-1b"]
}

variable "backend_container_port" {
  description = "Port the backend container listens on."
  type        = number
  default     = 8080
}

variable "backend_desired_count" {
  description = "Desired ECS task count for the backend service."
  type        = number
  default     = 2
}

variable "backend_cpu" {
  description = "Fargate task CPU units for the backend."
  type        = number
  default     = 512
}

variable "backend_memory" {
  description = "Fargate task memory (MiB) for the backend."
  type        = number
  default     = 1024
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_name" {
  description = "RDS database name."
  type        = string
  default     = "paymentflow"
}

variable "db_username" {
  description = "RDS master username. Password is intentionally NOT a variable default; it would come from AWS Secrets Manager / Parameter Store in a real deployment, never committed here."
  type        = string
  default     = "paymentflow"
}

variable "redis_node_type" {
  description = "ElastiCache Redis node type."
  type        = string
  default     = "cache.t4g.micro"
}
