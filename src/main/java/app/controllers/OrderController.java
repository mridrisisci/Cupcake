package app.controllers;

import app.entities.*;
import app.exceptions.DatabaseException;
import app.persistence.ConnectionPool;
import app.persistence.OrderMapper; // Sørg for at du har importeret din OrderMapper
import io.javalin.Javalin;
import io.javalin.http.Context; // Den korrekte Context import fra Javalin

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderController
{

    public static void addRoutes(Javalin app, ConnectionPool pool)
    {
        app.get("/ordrehistory", ctx -> showOrderHistory(ctx, pool));
        app.post("/addcupcake", ctx -> addCupcakeToBasket(ctx, pool));
        app.get("/basket", ctx -> ctx.render("basket.html") );
        app.get("/checkout", ctx -> ctx.render("checkout.html") );
        app.post("/checkout", ctx -> checkout(ctx, pool) );
    }

    private static void checkout(Context ctx, ConnectionPool pool) throws DatabaseException
    {
        List<OrderLine> orderLineList = ctx.sessionAttribute("orderlines");
        String username = ctx.formParam("username");
        String password = ctx.formParam("password");
        String currentUser = ctx.sessionAttribute("session.currentUser");



        LocalDate datePlaced = LocalDate.now();
        LocalDate datePaid = LocalDate.now();
        LocalDate dateCompleted = LocalDate.now().plusDays(2);

        String status = "betalt";
        int orderId = Integer.parseInt(currentUser);

        try
        {
            OrderMapper.newOrdersToOrdersTable(username, datePlaced, datePaid, dateCompleted, status, pool);
            OrderMapper.newOrderToOrderLines(orderId, orderLineList, pool);
        } catch (DatabaseException e)
        {
            throw new DatabaseException(e.getMessage());
        }
    }

    private static void addCupcakeToBasket(Context ctx, ConnectionPool pool) {
        List<OrderLine> orderLineList = ctx.sessionAttribute("orderlines");
        if (orderLineList == null) {
            orderLineList = new ArrayList<>();
        }

        String topFlavourName = ctx.formParam("chooseTop");
        String bottomFlavourName = ctx.formParam("chooseBottom");

        String quantityString = ctx.formParam("chooseAmount");
        if (quantityString == null) {
            ctx.sessionAttribute("error", "Please select a quantity.");
            CupcakeController.showFrontpage(ctx,pool);
            return;
        }
        int quantity = Integer.parseInt(quantityString);

        try {
            CupcakeFlavour topFlavour = OrderMapper.getCupcakeFlavour(topFlavourName, CupcakeType.TOP, pool);
            CupcakeFlavour bottomFlavour = OrderMapper.getCupcakeFlavour(bottomFlavourName, CupcakeType.BOTTOM, pool);
            Cupcake cupcake = new Cupcake(topFlavour, bottomFlavour);

            int orderId = 0;
            orderLineList.add(new OrderLine(
                    orderId,
                    quantity,
                    cupcake,
                    cupcake.getPrice()
            ));

            int ordersum = 0;
            for (OrderLine ol : orderLineList) {
                ordersum += ol.getPrice() * ol.getQuantity();
            }

            ctx.sessionAttribute("ordersum", ordersum);
            ctx.sessionAttribute("orderlines", orderLineList);
            ctx.redirect("/");
        } catch (DatabaseException e){
            ctx.attribute("message","Database error: " + e.getMessage());
            CupcakeController.showFrontpage(ctx,pool);
        }
    }

    private static void showOrderHistory(Context ctx, ConnectionPool pool)
    {
        List<Order> orders = new ArrayList<>();
        try
        {

            orders = OrderMapper.getOrders(pool);
        }
        catch (DatabaseException e)
        {

            ctx.attribute("message","Noget gik galt. " + e.getMessage());
        }


        ctx.attribute("orders", orders);

        // Render Thymeleaf-skabelonen
        ctx.render("/ordrehistory.html");
    }
}
