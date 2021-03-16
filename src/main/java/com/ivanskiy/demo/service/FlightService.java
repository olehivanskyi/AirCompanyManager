package com.ivanskiy.demo.service;

import com.ivanskiy.demo.domain.Flight;
import com.ivanskiy.demo.dto.FlightDto;
import com.ivanskiy.demo.entity.FlightStatusCode;
import com.ivanskiy.demo.repository.FlightRepository;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FlightService {
    FlightRepository flightRepository;
    AirCompanyService airCompanyService;
    TimeManager timeManager;

    public FlightService(FlightRepository flightRepository, AirCompanyService airCompanyService,
                         TimeManager timeManager) {
        this.flightRepository = flightRepository;
        this.airCompanyService = airCompanyService;
        this.timeManager = timeManager;
    }

    public void addFlight(FlightDto flightDto) {
        Flight flight = new Flight();
        flight.setFlightStatus("PENDING");
        flight.setAirCompanyId(flightDto.getAirCompanyId());
        flight.setAirplaneId(flightDto.getAirplaneId());
        flight.setDepartureCountry(flightDto.getDepartureCountry());
        flight.setDestinationCountry(flightDto.getDestinationCountry());
        flight.setDistance(flightDto.getDistance());
        flight.setEstimatedFlightTime(flightDto.getEstimatedFlightTime());
        flight.setEndedAt(flightDto.getEndedAt());
        flight.setDelayStartedAt(flightDto.getDelayStartedAt());
        flight.setCreatedAt(flightDto.getCreatedAt());
        flightRepository.save(flight);
    }

    public void updateFlight(Flight flight) {
        SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        transaction = session.beginTransaction();
        session.update(flight);
        transaction.commit();
        session.close();
    }

    public String getStatus(String status) {
        String realStatus = "PENDING";
        for(FlightStatusCode flightStatusCode : FlightStatusCode.values()) {
            if(status.equalsIgnoreCase(flightStatusCode.toString())) {
                realStatus = flightStatusCode.toString();
            }
        }
        return realStatus;
    }

    public List<Flight> getAllFlightByStatusCodeAndCompany(String status, String companyName) {
        int companyId = airCompanyService.getAirCompanyByName(companyName).getID();
        List<Flight> result = new ArrayList<>();
        List<Flight> flights = flightRepository.findFlightsByFlightStatus(getStatus(status));
        for (Flight flight : flights) {
            if(flight.getAirCompanyId()==companyId) {
                result.add(flight);
            }
        }
        return result;
    }

    public List<Flight> getAllFlightsThatFlyingLastsOver24Hours() {
        int countMinuteInOneDay = 1440;
        List<Flight> result = new ArrayList<>();
        List<Flight> flights = flightRepository.findFlightsByFlightStatus("ACTIVE");
        for (Flight flight : flights) {
            Date date = timeManager.getDateFromString(flight.getCreatedAt());
            long currentTime = new Date().getTime();
            int countMinuteAfterStart = timeManager.getMinuteFromMillisecond(currentTime - date.getTime());
            if(countMinuteAfterStart - countMinuteInOneDay > 0) {
                result.add(flight);
            }
        }
        return result;
    }

    public List<Flight> getCompletedWhichArrivedLate() {
        List<Flight> flights = flightRepository.findFlightsByFlightStatus("COMPLETED");
        List<Flight> result = new ArrayList<>();
        for(Flight flight : flights) {
            Date startTime = timeManager.getDateFromString(flight.getCreatedAt());
            Date endTime = timeManager.getDateFromString(flight.getEndedAt());
            int timeSpendToFlight = (int) timeManager.getTimeBeetwenStartAndEndFlight(startTime, endTime);
            if(flight.getEstimatedFlightTime() < timeSpendToFlight){
                result.add(flight);
            }
        }
        return result;
    }

    public void changeStatusCodeAndSetSomeTimeInfo(int flightId, String newStatusCode, String date) {
        Flight flight = new Flight();
        if(flightRepository.findFlightByID(flightId) != null) {
            flight = flightRepository.findFlightByID(flightId);
        }

        switch (getStatus(newStatusCode)) {
            case("DELAYED"):
                flight.setDelayStartedAt(date);
                flight.setFlightStatus("DELAYED");
                break;
            case("ACTIVE"):
                flight.setCreatedAt(date);
                flight.setFlightStatus("ACTIVE");
                break;
            case("COMPLETED"):
                flight.setEndedAt(date);
                flight.setFlightStatus("COMPLETED");
                break;
        }
        updateFlight(flight);
    }
}

