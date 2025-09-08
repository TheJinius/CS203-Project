
# this is for private subnets
# resource "aws_subnet" "private_rds_subnet_1" {
#   vpc_id                  = aws_vpc.vpc.id
#   availability_zone       = "ap-southeast-1a"
#   cidr_block              = "10.0.1.0/24"
#   map_public_ip_on_launch = false

#   tags = {
#     Name = "private_rds_subnet_1"
#   }
# }


# resource "aws_subnet" "private_rds_subnet_2" {
#   vpc_id                  = aws_vpc.vpc.id
#   availability_zone       = "ap-southeast-1b"
#   cidr_block              = "10.0.2.0/24"
#   map_public_ip_on_launch = false

#   tags = {
#     Name = "private_rds_subnet_2"
#   }
# }

# this is for public subnets (testing/dev)
resource "aws_subnet" "public_rds_subnet_1" {
  vpc_id                  = aws_vpc.vpc.id
  availability_zone       = "ap-southeast-1a"
  cidr_block              = "10.0.1.0/24"
  map_public_ip_on_launch = true

  tags = {
    Name = "public_rds_subnet_1"
  }
}


resource "aws_subnet" "public_rds_subnet_2" {
  vpc_id                  = aws_vpc.vpc.id
  availability_zone       = "ap-southeast-1b"
  cidr_block              = "10.0.2.0/24"
  map_public_ip_on_launch = true

  tags = {
    Name = "public_rds_subnet_2"
  }
}

resource "aws_db_subnet_group" "rds_public_subnet_group" {
  name = "rds_public_subnet_group"
  subnet_ids = [public_rds_subnet_1, public_rds_subnet_2]

  tags {
    Name = "rds_public_subnet_group"
  }
}