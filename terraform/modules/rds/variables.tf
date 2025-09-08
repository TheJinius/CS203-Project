variable "db_username" {
  description = "Database username"
  type        = string
}

variable "db_password" {
  description = "Database password"
  type        = string
  sensitive   = true
}

variable "rds_subnet_group_name" {
  description = "subnet group name for rds"
  type = string
}

variable "rds_sg_id" {
  description = "id for rds security group"
  type = string
}
