resource "aws_vpc" "vpc" {
  cidr_block           = "10.10.0.0/16" // ip address range from 10.0.0.0 to 10.0.255.255
  enable_dns_hostnames = true
}
