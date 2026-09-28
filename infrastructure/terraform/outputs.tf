# Illustrative outputs only — never populated by a real apply. See README.md.

output "alb_dns_name" {
  description = "Public DNS name of the ALB fronting the backend."
  value       = aws_lb.main.dns_name
}

output "rds_endpoint" {
  description = "RDS PostgreSQL connection endpoint."
  value       = aws_db_instance.main.address
}

output "redis_endpoint" {
  description = "ElastiCache Redis connection endpoint."
  value       = aws_elasticache_cluster.main.cache_nodes[0].address
}

output "ecs_cluster_name" {
  description = "Name of the ECS cluster."
  value       = aws_ecs_cluster.main.name
}
