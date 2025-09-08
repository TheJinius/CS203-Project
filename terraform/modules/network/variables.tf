variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to access the database"
  type        = list(string)
  default     = []
}

variable "Environment" {
    description = "current development environment"
    type = string
    default = "dev"
}

variable "vpc_id" {
    description = "id of vpc where rds is deployed"
    type = string
}