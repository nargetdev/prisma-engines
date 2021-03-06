pub trait NameNormalizer {
    fn camel_case(&self) -> String;
}

impl NameNormalizer for String {
    fn camel_case(&self) -> String {
        let mut c = self.chars();
        match c.next() {
            None => String::new(),
            Some(f) => f.to_lowercase().collect::<String>() + c.as_str(),
        }
    }
}

pub struct DefaultNames {}

impl DefaultNames {
    pub fn relation_name(from: &str, to: &str) -> String {
        if from < to {
            format!("{}To{}", from, to)
        } else {
            format!("{}To{}", to, from)
        }
    }
}
