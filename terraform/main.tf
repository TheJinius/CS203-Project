module "network" {
  source = "./modules/network"
}

module "rds" {
  source = "./modules/rds"
  rds_subnet_group_name = module.network.rds_subnet_group_name
  rds_sg_id = module.network.rds_subnet_sg_ids
}