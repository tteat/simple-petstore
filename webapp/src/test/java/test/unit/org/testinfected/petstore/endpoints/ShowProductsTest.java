package test.unit.org.testinfected.petstore.endpoints;

import com.google.common.base.Function;
import com.pyxis.petstore.domain.product.AttachmentStorage;
import com.pyxis.petstore.domain.product.Product;
import com.pyxis.petstore.domain.product.ProductCatalog;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsMapContaining;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testinfected.petstore.dispatch.Dispatch;
import org.testinfected.petstore.endpoints.ShowProducts;
import test.support.com.pyxis.petstore.builders.Builder;
import test.support.org.testinfected.petstore.web.MockRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.any;
import static test.support.com.pyxis.petstore.builders.Builders.build;
import static test.support.com.pyxis.petstore.builders.ProductBuilder.aProduct;

@RunWith(JMock.class)
public class ShowProductsTest {

    Mockery context = new JUnit4Mockery();
    ProductCatalog productCatalog = context.mock(ProductCatalog.class);
    AttachmentStorage attachmentStorage = context.mock(AttachmentStorage.class);
    ShowProducts showProducts = new ShowProducts(productCatalog, attachmentStorage);

    MockRequest request = new MockRequest();
    Dispatch.Response response = context.mock(Dispatch.Response.class);
    String keyword = "dogs";
    List<Product> searchResults = new ArrayList<Product>();


    @Before public void
    configureDefaultPhoto() {
        context.checking(new Expectations() {{
            allowing(attachmentStorage).getLocation(with("missing.png")); will(returnValue("/photos/missing.png"));
        }});
    }

    @Before public void
    prepareRequest() {
        request.addParameter("keyword", keyword);
    }

    @SuppressWarnings("unchecked")
    @Test public void
    rendersNoMatchPageWhenSearchYieldsNoResult() throws Exception {
        searchYieldsNothing();

        context.checking(new Expectations() {{
            oneOf(response).render(with("no-results"), with(hasEntry("keyword", keyword)));
        }});

        showProducts.process(request, response);
    }

    @SuppressWarnings("unchecked")
    @Test public void
    rendersProductsPageWithProductsInCatalogThatMatchKeyword() throws Exception {
        searchYields(
                aProduct().withNumber("LAB-1234").named("Labrador").describedAs("Friendly dog").withPhoto("labrador.png"),
                aProduct().describedAs("Guard dog"));

        context.checking(new Expectations() {{
            allowing(attachmentStorage).getLocation(with("labrador.png")); will(returnValue("/photos/labrador.png"));
            oneOf(response).render(with("products"), with(hasEntry("products", searchResults)));
        }});

        showProducts.process(request, response);
    }


    @Test public void
    makesMatchCountAvailableToView() throws Exception {
        searchYields(aProduct(), aProduct(), aProduct());

        context.checking(new Expectations() {{
            oneOf(response).render(with(view()), with(hasEntry("matchCount", 3)));
        }});

        showProducts.process(request, response);
    }

    @Test public void
    makesSearchKeywordAvailableToView() throws Exception {
        searchYields(aProduct());

        context.checking(new Expectations() {{
            oneOf(response).render(with(view()), with(hasEntry("keyword", keyword)));
        }});

        showProducts.process(request, response);
    }

    @Test public void
    makesImageResolverAvailableToView() throws Exception {
        searchYields(aProduct().withPhoto("photo.png"));

        context.checking(new Expectations() {{
            oneOf(response).render(with(view()), with(hasLambda("photo"))); will(call("photo", "photo.png"));
            oneOf(attachmentStorage).getLocation(with("photo.png"));
        }});

        showProducts.process(request, response);
    }

    private Matcher<Map<? extends String, ?>> hasEntry(String name, Object value) {
        return Matchers.hasEntry(name, value);
    }

    private Matcher<String> view() {
        return any(String.class);
    }

    @SuppressWarnings("unchecked")
    private void searchYieldsNothing() {
        searchYields();
    }

    private void searchYields(final Builder<Product>... products) {
        searchResults.addAll(build(products));
        context.checking(new Expectations() {{
            allowing(productCatalog).findByKeyword(keyword); will(returnValue(searchResults));
        }});
    }

    private Matcher<Map<? extends String, ? extends Function>> hasLambda(String name) {
        return new IsMapContaining<String, Function>(equalTo(name), any(Function.class));
    }

    private Action call(String key, String input) {
        return new CallLambda(key, input);
    }

    public class CallLambda implements Action {
        private final String lambda;
        private String input;

        public CallLambda(String lambda, String input) {
            this.lambda = lambda;
            this.input = input;
        }

        public void describeTo(Description description) {
            description.appendText("calls " + lambda + " with ").appendText(input);
        }

        @SuppressWarnings("unchecked")
        public Object invoke(Invocation invocation) throws Throwable {
            Map<String, ?> context = ((Map<String, ?>) invocation.getParameter(1));
            ((Function) context.get(lambda)).apply(input);
            return null;
        }
    }
}
