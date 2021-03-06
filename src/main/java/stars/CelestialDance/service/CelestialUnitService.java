package stars.CelestialDance.service;

import org.springframework.stereotype.Service;
import stars.CelestialDance.model.OrbitRadius;
import stars.CelestialDance.utils.apiConverter.BodyDataConverter;
import stars.CelestialDance.model.Body;
import stars.CelestialDance.model.CelestialUnit;
import stars.CelestialDance.utils.Utils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CelestialUnitService {

    private final BodyService bodyService;
    private final OrbitRadiusService radiusService;

    public CelestialUnitService(BodyService bodyService, OrbitRadiusService radiusService) {
        this.bodyService = bodyService;
        this.radiusService = radiusService;
    }

    public void deleteById(int id) {
        if (radiusService.findById(id).isPresent()) {
            radiusService.deleteById(id);
        }
        if (bodyService.findById(id).isPresent()) {
            bodyService.deleteById(id);
        }
    }

    public void deleteAll() {
        List<Body> bodies = bodyService.findAll();
        for(Body body:bodies){
            bodyService.delete(body);
        }
        List<OrbitRadius> radiusList = radiusService.findAll();
        for(OrbitRadius radius:radiusList){
            radiusService.delete(radius);
        }
    }

    public List<CelestialUnit> processMultipleData(List<BodyDataConverter> multipleData, String filter) {
        if (filter.equals("planets")) {
            return multipleData.stream()
                    .filter(d -> d.getIsPlanet().equals("true"))
                    .map(BodyDataConverter::convertToCelestialUnit)
                    .collect(Collectors.toList());
        }
        if (filter.equals("none")) {
            return multipleData.stream()
                    .map(BodyDataConverter::convertToCelestialUnit)
                    .collect(Collectors.toList());
        }
        return null;
    }

    public void setOnePrimaryBody(List<BodyDataConverter> multipleData, int id) {
        String name = bodyService.findById(id).get().getName();
        Optional<Body> optionalBody = bodyService.findByName(name);
        if (optionalBody.isEmpty()) {
            return;
        }
        Body body = optionalBody.get();
        for (BodyDataConverter data : multipleData) {
            if (data.getIsPlanet().equals("true") && data.getEnglishName().equals(name)) {
                Optional<Body> optionalSun = bodyService.findByName("Sun");
                if (optionalSun.isPresent()) {
                    body.setPrimaryBodyId(optionalSun.get().getId());
                    bodyService.save(body);
                }
                return;
            }

            if (data.getMoons() != null) {
                List<String> moonNames = data.getMoonNames();
                for (String moonName : moonNames) {
                    if (moonName.equals(name)) {
                        Optional<Body> optionalPrimaryBody = bodyService.findByName(data.getEnglishName());
                        if (optionalPrimaryBody.isPresent()) {
                            body.setPrimaryBodyId(optionalPrimaryBody.get().getId());
                            bodyService.save(body);
                            return;
                        }
                    }
                }
            }
        }
    }


    public void setAllPrimaryBodies(List<BodyDataConverter> multipleData) {
        int sunId = bodyService.findByName("Sun").get().getId();

        for (BodyDataConverter data : multipleData) {
            if (data.getIsPlanet() == "true") {
                Optional<Body> optionalBody = bodyService.findByName(data.getEnglishName());
                if (optionalBody.isPresent()) {
                    Body body = optionalBody.get();
                    body.setPrimaryBodyId(sunId);
                    bodyService.save(body);
                }
            }
            if (data.getMoons() != null) {
                Optional<Body> optionalPrimaryBody = bodyService.findByName(data.getEnglishName());
                if (optionalPrimaryBody.isPresent()) {
                    int primaryBodyId = optionalPrimaryBody.get().getId();
                    List<String> moonNames = data.getMoonNames();
                    for (String moonName : moonNames) {
                        Optional<Body> optionalBody = bodyService.findByName(moonName);
                        if (optionalBody.isPresent()) {
                            Body body = optionalBody.get();
                            body.setPrimaryBodyId(primaryBodyId);
                            bodyService.save(body);
                        }
                    }
                }
            }
        }
    }

    public void saveNewUnit(CelestialUnit unit) {
        if (bodyService.findByName(unit.getBody().getName()).isEmpty()) {
            int id = bodyService.saveNewBody(unit.getBody());

            if (unit.getOrbitRadius() != null) {
                unit.getOrbitRadius().setId(id);
                radiusService.save(unit.getOrbitRadius());
            }
        }
    }


    public double calculateVelocity(CelestialUnit unit, String maxOrMin) {

        //vmax = sqrt( (1+e) * GM / rmin )
        //vmin = sqrt( (1-e) * GM / rmax )

        float e = unit.getOrbitRadius().getEccentricity();
        long rmin = unit.getOrbitRadius().getRMin();
        long rmax = unit.getOrbitRadius().getRMax();
        double GM = Utils.calcStandGravParam(bodyService.findById(unit.getBody().getPrimaryBodyId()).get());
        double v = 0;

        if (maxOrMin.equals("max")) {
            v = Math.sqrt((1 + e) * GM / rmin);
        } else if (maxOrMin.equals("min")) {
            v = Math.sqrt((1 - e) * GM / rmax);
        }
        return v;
    }


    public CelestialUnit setUnitOnMap(CelestialUnit unit, String apOrPer) {
        Body body2 = bodyService.findById(unit.getBody().getPrimaryBodyId()).get();

        long R = 0;
        double v = 0;
        if (apOrPer.equals("ap")) {
            R = unit.getOrbitRadius().getRMax();
            v = calculateVelocity(unit, "min");
        } else if (apOrPer.equals("per")) {
            R = unit.getOrbitRadius().getRMin();
            v = calculateVelocity(unit, "max");
        }
        double angle = Utils.getRanNumber(0, 2 * Math.PI);
        unit.getBody().setPosX((float) (body2.getPosX() + Math.sin(angle) * R));
        unit.getBody().setPosY((float) (body2.getPosY() - Math.cos(angle) * R));

        unit.getBody().setVelX((float) (-v * Math.cos(angle) + body2.getVelX()));
        unit.getBody().setVelY((float) (-v * Math.sin(angle) + body2.getVelY()));

        return unit;
    }

    public CelestialUnit findById(int id) {
        CelestialUnit unit = new CelestialUnit();
        if (bodyService.findById(id).isPresent()) {
            unit.setBody(bodyService.findById(id).get());
        }
        if (radiusService.findById(id).isPresent()) {
            unit.setOrbitRadius(radiusService.findById(id).get());
        }

        return unit;
    }

    public CelestialUnit findByName(String name) {
        CelestialUnit unit = new CelestialUnit();
        if (bodyService.findByName(name).isPresent()) {
            unit.setBody(bodyService.findByName(name).get());
            int id = unit.getBody().getId();
            if (radiusService.findById(id).isPresent()) {
                unit.setOrbitRadius(radiusService.findById(id).get());
            }
        }
        return unit;
    }

    public void save(CelestialUnit unit) {
        bodyService.save(unit.getBody());
        int id = unit.getBody().getId();

        if (unit.getOrbitRadius() != null) {
            unit.getOrbitRadius().setId(id);
            radiusService.save(unit.getOrbitRadius());
        }

    }

}
