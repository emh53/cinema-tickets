package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final long ACCOUNT_ID = 1L;

    @Mock
    private TicketPaymentService ticketPaymentService;

    @Mock
    private SeatReservationService seatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @ParameterizedTest(name = "accountId={0}")
    @MethodSource("invalidAccountIds")
    void invalidAccountId_throwsInvalidPurchaseException(Long invalidAccountId) {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        assertThatThrownBy(() -> ticketService.purchaseTickets(invalidAccountId, request))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    static Stream<Long> invalidAccountIds() {
        return Stream.of(0L, -1L, null);
    }

    @ParameterizedTest
    @EnumSource(TicketTypeRequest.Type.class)
    void noTicketsRequested_throwsInvalidPurchaseException(TicketTypeRequest.Type type) {
        TicketTypeRequest request = new TicketTypeRequest(type, 0);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, request))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @ParameterizedTest
    @EnumSource(TicketTypeRequest.Type.class)
    void negativeTicketsRequested_throwsInvalidPurchaseException(TicketTypeRequest.Type type) {
        TicketTypeRequest request = new TicketTypeRequest(type, -1);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, request))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @ParameterizedTest
    @EnumSource(value = TicketTypeRequest.Type.class, names = {"INFANT", "CHILD"})
    void negativeTicketsWithPositiveAdultTicketsRequested_throwsInvalidPurchaseException(TicketTypeRequest.Type type) {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest negative = new TicketTypeRequest(type, -1);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, request, negative))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    void multipleRequestsWithNoTicketsRequested_throwsInvalidPurchaseException() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 0);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 0);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, adultRequest, childRequest, infantRequest))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    void multipleRequestsWithNoAdultTicketRequest_throwsInvalidPurchaseException() {
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, childRequest, infantRequest))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    void multipleRequestsWithNoAdultTicketsRequested_throwsInvalidPurchaseException() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, adultRequest, childRequest, infantRequest))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    void moreInfantsThanAdultsAvailableToShareSeats_throwsInvalidPurchaseException() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, adultRequest, infantRequest))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    void adultRequestsWithNoChildAndInfantRequested_validRequest() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 0);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 0);

        ticketService.purchaseTickets(ACCOUNT_ID, adultRequest, childRequest, infantRequest);
    }

    @Test
    void moreThat25AdultTicketsRequest_throwsInvalidPurchaseException() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, adultRequest))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    void moreThat25MixedTicketsRequest_throwsInvalidPurchaseException() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 6);

        assertThatThrownBy(() -> ticketService.purchaseTickets(ACCOUNT_ID, adultRequest, childRequest, infantRequest))
                .isInstanceOf(InvalidPurchaseException.class);
    }

    @Test
    void oneAdult_paymentRequestCalledWithCorrectValue() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        ticketService.purchaseTickets(ACCOUNT_ID, adultRequest);

        verify(ticketPaymentService, only()).makePayment(ACCOUNT_ID, 25);
    }

    @ParameterizedTest
    @MethodSource("paymentValues")
    void multipleTickets_paymentRequestCalledWithCorrectValue(int adults, int children, int infants, int total) {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, adults);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, children);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, infants);

        ticketService.purchaseTickets(ACCOUNT_ID, adultRequest, childRequest, infantRequest);

        verify(ticketPaymentService, only()).makePayment(ACCOUNT_ID, total);
    }

    public static Stream<Arguments> paymentValues() {
        return Stream.of(
                Arguments.of(1, 0, 0, 25),
                Arguments.of(5, 0, 0, 125),
                Arguments.of(1, 1, 1, 40),
                Arguments.of(1, 1, 0, 40),
                Arguments.of(1, 5, 0, 100),
                Arguments.of(1, 0, 1, 25),
                Arguments.of(5, 0, 5, 125),
                Arguments.of(10, 10, 5, 400)
        );
    }

    @Test
    void oneAdult_seatReservationCalledWithCorrectValue() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);

        ticketService.purchaseTickets(ACCOUNT_ID, adultRequest);

        verify(seatReservationService, only()).reserveSeat(ACCOUNT_ID, 1);
    }

    @ParameterizedTest
    @MethodSource("reservationValues")
    void multipleTickets_seatReservationCalledWithCorrectValue(int adults, int children, int infants, int total) {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, adults);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, children);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, infants);

        ticketService.purchaseTickets(ACCOUNT_ID, adultRequest, childRequest, infantRequest);

        verify(seatReservationService, only()).reserveSeat(ACCOUNT_ID, total);
    }

    public static Stream<Arguments> reservationValues() {
        return Stream.of(
                Arguments.of(1, 0, 0, 1),
                Arguments.of(5, 0, 0, 5),
                Arguments.of(1, 1, 1, 2),
                Arguments.of(1, 1, 0, 2),
                Arguments.of(1, 5, 0, 6),
                Arguments.of(1, 0, 1, 1),
                Arguments.of(5, 0, 5, 5),
                Arguments.of(10, 10, 5, 20)
        );
    }
}