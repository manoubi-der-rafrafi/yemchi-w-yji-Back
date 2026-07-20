package com.transport.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.transport.transport.model.Commande;

@Service
public class CommandeGeographyService {

    public void enrichCommandeGeography(Commande commande) {
        if (commande == null) {
            return;
        }

        hydrateZonesFromCoordinates(
                commande.getLatitudeDepart(),
                commande.getLongitudeDepart(),
                commande.getZonePrincipaleDepart(),
                commande.getSousZoneDepart(),
                commande::setZonePrincipaleDepart,
                commande::setSousZoneDepart);
        hydrateZonesFromCoordinates(
                commande.getLatitudeDestination(),
                commande.getLongitudeDestination(),
                commande.getZonePrincipaleArrivee(),
                commande.getSousZoneArrivee(),
                commande::setZonePrincipaleArrivee,
                commande::setSousZoneArrivee);
    }

    private void hydrateZonesFromCoordinates(
            Double latitude,
            Double longitude,
            Commande.Zone currentZone,
            Commande.SousZone currentSousZone,
            java.util.function.Consumer<Commande.Zone> zoneSetter,
            java.util.function.Consumer<Commande.SousZone> sousZoneSetter) {
        if (currentZone != null && currentSousZone != null) {
            return;
        }
        if (!hasUsableCoordinates(latitude, longitude)) {
            return;
        }

        ZoneMatch match = resolveZone(latitude, longitude);
        if (match == null) {
            return;
        }
        if (currentZone == null) {
            zoneSetter.accept(match.zone());
        }
        if (currentSousZone == null) {
            sousZoneSetter.accept(match.sousZone());
        }
    }

    private boolean hasUsableCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }
        return Math.abs(latitude) > 0.000001d || Math.abs(longitude) > 0.000001d;
    }

    private ZoneMatch resolveZone(double latitude, double longitude) {
        for (ZoneBounds bounds : ZONE_BOUNDS) {
            if (bounds.matches(latitude, longitude)) {
                return new ZoneMatch(bounds.zone(), bounds.sousZone());
            }
        }
        return null;
    }

    private record ZoneMatch(Commande.Zone zone, Commande.SousZone sousZone) {}

    private record ZoneBounds(
            Commande.Zone zone,
            Commande.SousZone sousZone,
            double latMin,
            double latMax,
            double lonMin,
            double lonMax) {
        boolean matches(double latitude, double longitude) {
            return latitude >= latMin && latitude <= latMax && longitude >= lonMin && longitude <= lonMax;
        }
    }

    private static final List<ZoneBounds> ZONE_BOUNDS = List.of(
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.TUNIS, 36.70, 36.95, 10.00, 10.45),
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.ARIANA, 36.75, 37.10, 10.00, 10.45),
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.BEN_AROUS, 36.55, 36.85, 10.05, 10.45),
            new ZoneBounds(Commande.Zone.GRAND_TUNIS, Commande.SousZone.MANOUBA, 36.60, 36.98, 9.85, 10.25),
            new ZoneBounds(Commande.Zone.NORD_EST, Commande.SousZone.BIZERTE, 37.00, 37.55, 9.30, 10.35),
            new ZoneBounds(Commande.Zone.NORD_EST, Commande.SousZone.NABEUL, 36.15, 36.95, 10.25, 11.20),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.BEJA, 36.50, 37.20, 8.60, 9.60),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.JENDOUBA, 36.50, 37.10, 8.30, 9.20),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.KEF, 35.90, 36.80, 8.20, 9.10),
            new ZoneBounds(Commande.Zone.NORD_OUEST, Commande.SousZone.SILIANA, 35.70, 36.40, 8.70, 9.80),
            new ZoneBounds(Commande.Zone.CENTRE, Commande.SousZone.ZAGHOUAN, 36.10, 36.60, 9.90, 10.60),
            new ZoneBounds(Commande.Zone.CENTRE, Commande.SousZone.KAIROUAN, 35.20, 36.10, 9.50, 10.50),
            new ZoneBounds(Commande.Zone.CENTRE_OUEST, Commande.SousZone.KASSERINE, 34.80, 35.70, 8.20, 9.50),
            new ZoneBounds(Commande.Zone.CENTRE_OUEST, Commande.SousZone.SIDI_BOUZID, 34.60, 35.60, 8.80, 10.00),
            new ZoneBounds(Commande.Zone.SAHEL, Commande.SousZone.SOUSSE, 35.70, 36.20, 10.30, 10.90),
            new ZoneBounds(Commande.Zone.SAHEL, Commande.SousZone.MONASTIR, 35.50, 35.90, 10.60, 11.20),
            new ZoneBounds(Commande.Zone.SAHEL, Commande.SousZone.MAHDIA, 35.20, 35.70, 10.60, 11.30),
            new ZoneBounds(Commande.Zone.SFAX, Commande.SousZone.SFAX, 34.50, 35.10, 10.40, 11.20),
            new ZoneBounds(Commande.Zone.SUD_EST, Commande.SousZone.GABES, 33.70, 34.30, 9.80, 10.40),
            new ZoneBounds(Commande.Zone.SUD_EST, Commande.SousZone.MEDENINE, 33.00, 33.80, 10.00, 11.20),
            new ZoneBounds(Commande.Zone.SUD_EST, Commande.SousZone.TATAOUINE, 31.50, 33.40, 9.50, 11.20),
            new ZoneBounds(Commande.Zone.SUD_OUEST, Commande.SousZone.TOZEUR, 33.50, 34.20, 7.30, 8.40),
            new ZoneBounds(Commande.Zone.SUD_OUEST, Commande.SousZone.KEBILI, 33.10, 34.00, 8.20, 9.30),
            new ZoneBounds(Commande.Zone.SUD_OUEST, Commande.SousZone.GAFSA, 33.70, 34.80, 8.30, 9.30));
}
