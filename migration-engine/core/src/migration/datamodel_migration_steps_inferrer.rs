use migration_connector::steps::*;
use prisma_datamodel::*;

pub trait DataModelMigrationStepsInferrer {
    fn infer(previous: Schema, next: Schema) -> Vec<MigrationStep>;
}

impl DataModelMigrationStepsInferrer for DataModelMigrationStepsInferrerImpl {
    fn infer(previous: Schema, next: Schema) -> Vec<MigrationStep> {
        let inferrer = DataModelMigrationStepsInferrerImpl { previous, next };
        inferrer.infer_internal()
    }
}

#[allow(dead_code)]
pub struct DataModelMigrationStepsInferrerImpl {
    previous: Schema,
    next: Schema,
}

impl DataModelMigrationStepsInferrerImpl {
    fn infer_internal(&self) -> Vec<MigrationStep> {
        vec![]
    }
}
