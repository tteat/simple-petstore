package test.com.pyxis.petstore.domain;

import com.pyxis.petstore.domain.Product;
import org.junit.Test;

import static com.pyxis.matchers.validation.ViolationMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static test.support.com.pyxis.petstore.builders.ProductBuilder.aProduct;
import static test.support.com.pyxis.petstore.validation.ValidationOf.validationOf;

public class ProductTest {

    @Test public void
    isInvalidWithoutAName() {
        Product aProductWithoutAName = aProduct().withName(null).build();
        assertThat(validationOf(aProductWithoutAName), violates(on("name"), withError("NotNull")));
    }

    @Test public void
    isInvalidWithoutANumber() {
        Product aProductWithoutANumber = aProduct().withNumber(null).build();
        assertThat(validationOf(aProductWithoutANumber), violates(on("number"), withError("NotNull")));
    }

    @Test public void
    isValidWithANameAndANumber() {
        Product aValidProduct = aProduct().build();
        assertThat(validationOf(aValidProduct), succeeds());
    }
}
