# Spring Data JPA Specification -- Notes

## 1. Why do we need Specification?

Imagine an e-commerce application with a `product` table containing:

-   `id`
-   `name`
-   `category`
-   `price`
-   `brand`

The client may want to search using different combinations of filters:

``` http
GET /search?category=Mobiles
GET /search?brand=Apple
GET /search?category=Mobiles&brand=Samsung
GET /search?category=Mobiles&brand=Apple&name=iPhone
```

### Approach 1: Fetch everything and filter in Java

One approach is to fetch all products from the database and filter them
using Java Streams.

``` java
List<Product> products = productRepository.findAll();

List<Product> result = products.stream()
        .filter(product -> product.getCategory().equals("Mobiles"))
        .toList();
```

This can be inefficient for a large dataset. For example, if the
database contains one million products but the client needs only ten,
fetching every product into application memory just to filter them is
wasteful.

### Better approach: Filter in the database

Ideally, the database should perform the filtering and return only
matching records.

``` sql
SELECT *
FROM product
WHERE category = 'Mobiles';
```

Spring Data JPA Specification lets us build these filtering conditions
programmatically and execute them through the repository.

------------------------------------------------------------------------

## 2. What is Specification?

`Specification<T>` is an interface provided by Spring Data JPA.

It represents a reusable piece of query logic. It allows us to define a
condition for an entity and combine that condition with other
conditions.

A Specification implements a method named `toPredicate()`.

Conceptually, its method looks like this:

``` java
Predicate toPredicate(
        Root<T> root,
        CriteriaQuery<?> query,
        CriteriaBuilder cb
);
```

For our project, `T` is `Product`, so we use `Specification<Product>`.

A Specification is the recipe for creating a query condition. The
`Predicate` is the condition produced by that recipe.

------------------------------------------------------------------------

## 3. Understand the three arguments

When implementing `toPredicate()`, JPA provides three important
arguments.

### A. Root`<T>`{=html} -- access entity attributes

`Root<Product>` represents the `Product` entity in the query.

We use it to refer to entity attributes:

``` java
root.get("category")
root.get("brand")
root.get("price")
root.get("name")
```

For example:

``` java
root.get("price")
```

This creates a Criteria API expression representing the `price`
attribute. It does not fetch a particular product's price into Java
memory.

The names passed to `root.get()` are Java entity attribute names, not
necessarily the database column names.

### B. CriteriaQuery\<?\> -- represents the overall query

`CriteriaQuery<?>` represents the query being constructed.

It provides methods for modifying parts of the query, such as:

-   `where()` -- filtering conditions
-   `orderBy()` -- sorting
-   `groupBy()` -- grouping
-   `having()` -- filtering grouped results
-   `distinct()` -- removing duplicate results

For simple Specifications, we often do not need to modify `query`
directly.

### C. CriteriaBuilder -- builds query expressions and conditions

`CriteriaBuilder` is a toolbox for constructing Criteria API expressions
and Predicates.

Some useful methods:

  ------------------------------------------------------------------------------------
  Method                     Purpose                 Example condition
  -------------------------- ----------------------- ---------------------------------
  `equal()`                  Equal to                `price = 70000`

  `notEqual()`               Not equal to            `brand <> 'Apple'`

  `greaterThan()`            Greater than            `price > 50000`

  `greaterThanOrEqualTo()`   Greater than or equal   `price >= 50000`
                             to                      

  `lessThan()`               Less than               `price < 50000`

  `lessThanOrEqualTo()`      Less than or equal to   `price <= 50000`

  `between()`                Within a range          `price BETWEEN 20000 AND 50000`

  `like()`                   Pattern matching        `name LIKE '%phone%'`

  `isNull()`                 Is null                 `brand IS NULL`

  `isNotNull()`              Is not null             `brand IS NOT NULL`

  `and()`                    Combine conditions with `A AND B`
                             AND                     

  `or()`                     Combine conditions with `A OR B`
                             OR                      

  `not()`                    Negate a condition      `NOT A`

  `lower()`                  Convert expression to   `LOWER(name)`
                             lowercase               

  `conjunction()`            Always-true condition   `TRUE`

  `disjunction()`            Always-false condition  `FALSE`
  ------------------------------------------------------------------------------------

------------------------------------------------------------------------

## 4. What is Predicate?

A `Predicate` represents a Boolean condition in a database query.

For example:

``` java
cb.equal(root.get("category"), "Mobiles");
```

This constructs a Predicate representing:

``` sql
category = 'Mobiles'
```

Another example:

``` java
cb.greaterThan(root.get("price"), 50000);
```

This constructs a Predicate representing:

``` sql
price > 50000
```

Important distinction:

-   A Predicate is not a Java function that directly checks a `Product`
    object in memory.
-   It is a Criteria API condition that JPA can translate into SQL.
-   The database evaluates the condition against records and returns
    matching rows.

------------------------------------------------------------------------

## 5. Create individual Specifications

Create a class named `ProductSpecification`.

``` java
package com.example.SpecificationAPIDemo.specification;

import org.springframework.data.jpa.domain.Specification;

import com.example.SpecificationAPIDemo.model.Product;

public class ProductSpecification {

    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) ->
                cb.equal(root.get("category"), category);
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, cb) ->
                cb.equal(root.get("brand"), brand);
    }

    public static Specification<Product> hasName(String name) {
        return (root, query, cb) ->
                cb.like(
                    cb.lower(root.get("name")),
                    "%" + name.toLowerCase() + "%"
                );
    }

    public static Specification<Product> hasPrice(Integer price) {
        return (root, query, cb) ->
                cb.equal(root.get("price"), price);
    }
}
```

Each method returns a Specification. The lambda implements the
Specification's `toPredicate()` method.

For example, `hasCategory("Mobiles")` creates a Specification whose
Predicate represents:

``` sql
category = 'Mobiles'
```

The `hasName()` method uses `LIKE` with `%` on both sides, so a search
for `iPhone` can match names such as `iPhone 16` and `iPhone 17`.

------------------------------------------------------------------------

## 6. Understand the lambda

This lambda:

``` java
Specification<Product> spec = (root, query, cb) ->
        cb.conjunction();
```

is shorthand for an anonymous class:

``` java
Specification<Product> spec = new Specification<Product>() {

    @Override
    public Predicate toPredicate(
            Root<Product> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb) {

        return cb.conjunction();
    }
};
```

The lambda is possible because `Specification` is a functional
interface: it has one abstract method to implement.

------------------------------------------------------------------------

## 7. What is cb.conjunction()?

``` java
cb.conjunction()
```

returns a Predicate that is always true.

It is useful as an initial condition when building a Specification
dynamically.

For example, we start with:

``` java
Specification<Product> spec =
        (root, query, cb) -> cb.conjunction();
```

This means we begin with no restrictive filter. We can then add
conditions as the client provides filters.

`cb.disjunction()` is the opposite: it creates an always-false
Predicate.

------------------------------------------------------------------------

## 8. Combine Specifications dynamically

Example controller logic:

``` java
Specification<Product> spec =
        (root, query, cb) -> cb.conjunction();

if (request.getCategory() != null) {
    spec = spec.and(
            ProductSpecification.hasCategory(request.getCategory())
    );
}

if (request.getBrand() != null) {
    spec = spec.and(
            ProductSpecification.hasBrand(request.getBrand())
    );
}

if (request.getName() != null) {
    spec = spec.and(
            ProductSpecification.hasName(request.getName())
    );
}

if (request.getPrice() != null) {
    spec = spec.and(
            ProductSpecification.hasPrice(request.getPrice())
    );
}
```

Each `if` checks whether the client supplied that filter.

-   If category is present, add the category Specification.
-   If brand is present, add the brand Specification.
-   If name is present, add the name Specification.
-   If price is present, add the price Specification.

### Example

Suppose the client sends:

``` http
GET /search/specification?category=Mobiles&brand=Samsung
```

The final logical condition is:

``` sql
WHERE category = 'Mobiles'
  AND brand = 'Samsung'
```

The initial `TRUE` condition does not change the result:

``` text
TRUE AND category = 'Mobiles' AND brand = 'Samsung'
```

`Specification.and()` and `Specification.or()` compose Specification
objects. Their Predicates are constructed when the repository evaluates
the Specification.

Use `.or()` when the requirement is to match either condition.

For example:

``` java
spec = ProductSpecification.hasBrand("Apple")
        .or(ProductSpecification.hasBrand("Samsung"));
```

Conceptually:

``` sql
WHERE brand = 'Apple'
   OR brand = 'Samsung'
```

------------------------------------------------------------------------

## 9. Repository configuration

The repository must extend both `JpaRepository` and
`JpaSpecificationExecutor`.

``` java
package com.example.SpecificationAPIDemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.SpecificationAPIDemo.model.Product;

public interface ProductRepository extends
        JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {
}
```

-   `JpaRepository` provides CRUD operations.
-   `JpaSpecificationExecutor` provides methods to execute
    Specifications, such as `findAll(spec)`.

------------------------------------------------------------------------

## 10. Execute the Specification

The service can receive the Specification and pass it to the repository:

``` java
public List<Product> getSpecificProducts(Specification<Product> spec) {
    return productRepository.findAll(spec);
}
```

When `findAll(spec)` is called:

1.  Spring Data JPA evaluates the Specification.
2.  The Specification's `toPredicate()` method produces a Predicate.
3.  JPA uses the Criteria API to construct the database query.
4.  The query is executed by the database.
5.  Matching products are returned.

The filtering happens in the database rather than fetching every product
and filtering the full list in Java.

------------------------------------------------------------------------

## 11. Overall flow

``` text
Client sends search request
          |
          v
Controller reads query parameters
          |
          v
Builds a Specification<Product>
          |
          v
Adds available filters using .and() / .or()
          |
          v
Passes Specification to Service
          |
          v
Repository: findAll(spec)
          |
          v
Specification.toPredicate() creates Predicate
          |
          v
JPA Criteria API constructs SQL
          |
          v
Database executes SQL
          |
          v
Matching products are returned
```

------------------------------------------------------------------------

## 12. Test URLs

Assuming the endpoint is `/search/specification`:

``` http
GET http://localhost:8080/search/specification
GET http://localhost:8080/search/specification?category=Mobiles
GET http://localhost:8080/search/specification?brand=Apple
GET http://localhost:8080/search/specification?name=iPhone
GET http://localhost:8080/search/specification?category=Mobiles&brand=Samsung
GET http://localhost:8080/search/specification?category=Mobiles&brand=Apple&name=iPhone
```

Query parameters can be combined using `&`.

------------------------------------------------------------------------

## 13. Important points to remember

1.  **Specification** is a reusable recipe for constructing query
    conditions.
2.  **Predicate** represents the actual Boolean condition in the
    Criteria API.
3.  **Root** provides access to entity attributes.
4.  **CriteriaQuery** represents the overall query being constructed.
5.  **CriteriaBuilder** helps construct expressions and Predicates.
6.  **conjunction()** creates an always-true condition, useful as a
    starting point.
7.  **and() / or()** combine Specifications.
8.  **JpaSpecificationExecutor** enables repository methods such as
    `findAll(spec)`.
9.  Specifications help build dynamic queries without creating a
    repository method for every filter combination.
10. Database-side filtering avoids loading all candidate records into
    application memory. For large result sets, also consider pagination
    and appropriate indexes.

## 14. Similar concepts in other ecosystems

The idea of building database queries dynamically is not unique to
Spring Data JPA.

-   **Java / Spring:** Spring Data JPA Specification, built on the
    Jakarta Persistence Criteria API.
-   **Python:** SQLAlchemy SQL Expression Language and ORM query
    expressions.
-   **.NET:** Entity Framework Core LINQ queries and expression trees.

These are related approaches, not identical APIs.

------------------------------------------------------------------------

## 15. Suggested next topics to practise

-   Add minimum and maximum price filters using `greaterThanOrEqualTo()`
    and `lessThanOrEqualTo()`.
-   Combine conditions using both `AND` and `OR`.
-   Add sorting and pagination.
-   Explore joins when filtering by related entities.
-   Enable Hibernate SQL logging to inspect the generated SQL.
