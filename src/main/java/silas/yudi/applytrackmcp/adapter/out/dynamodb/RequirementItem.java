package silas.yudi.applytrackmcp.adapter.out.dynamodb;

import silas.yudi.applytrackmcp.domain.model.Importance;
import silas.yudi.applytrackmcp.domain.model.Requirement;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

/** Requisito gravado dentro do item da candidatura. */
@DynamoDbBean
public class RequirementItem {

    private String name;
    private String level;
    private Importance importance;

    static RequirementItem from(Requirement requirement) {
        var item = new RequirementItem();
        item.setName(requirement.name());
        item.setLevel(requirement.level());
        item.setImportance(requirement.importance());
        return item;
    }

    Requirement toDomain() {
        return new Requirement(name, level, importance);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Importance getImportance() {
        return importance;
    }

    public void setImportance(Importance importance) {
        this.importance = importance;
    }
}
