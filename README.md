Khi bắt đầu chạy, vào folder và chạy lệnh: 
```bash
docker compose up -d
```
Code yêu cầu:
- Comment phương thức đó làm gì ngay trên method đó
- Sử dụng Lombok, có thể dùng mapstruct để map object
- Xử lý exception tập trung tại exception trong common module
- Những config nào chung thì config trong common module
- Code phải clean và dễ hiểu, xử lý logic nào thì code trong module đó tránh ảnh hưởng người khác
- Sau khi code xong, push lên github và báo mọi người
- Các api tạo ra cần test và hãy viết mô tả api đó làm gì
# Cấu trúc dự án
```
cookshare/
├── pom.xml (parent)
├── README.md
└── modules/
    ├── authentication/
    │    ├── entity/
    │    │   ├── User.java
    ├── user/
    │   ├── entity/
    │   ├── Follow.java
    │   ├── FollowId.java
    │   ├── Notification.java
    │   ├── NotificationType.java (enum)
    │   ├── RelatedType.java (enum)
    │   ├── ActivityLog.java
    │   ├── ActivityType.java (enum)
    │   ├── Collection.java
    │   ├── CollectionRecipe.java
    │   └── CollectionRecipeId.java
    ├── recipe-management/
    │    ├── entity/
    │    │   ├── Recipe.java
    │    │   ├── Difficulty.java (enum)
    │    │   ├── RecipeStep.java
    │    │   ├── RecipeIngredient.java
    │    │   ├── RecipeIngredientId.java
    │    │   ├── Ingredient.java
    │    │   ├── Category.java
    │    │   ├── RecipeCategory.java
    │    │   ├── RecipeCategoryId.java
    │    │   ├── Tag.java
    │    │   ├── RecipeTag.java
    │    │   └── RecipeTagId.java
    ├── interaction/
    │    │   ├── entity/
    │    │   ├── RecipeLike.java
    │    │   ├── RecipeLikeId.java
    │    │   ├── RecipeRating.java
    │    │   └── SearchHistory.java
    ├── recommendation/
    ├── common/
    │    │   ├── config/
    │    │   ├── exception/
    │    │   ├── util/
    └── system/
        ├── entity/
        │   ├── Report.java
        │   ├── ReportType.java (enum)
        │   └── ReportStatus.java (enum)
