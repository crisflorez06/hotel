DROP TRIGGER IF EXISTS trg_estancia_habitacion_unica_activa_ins$$
DROP TRIGGER IF EXISTS trg_estancia_habitacion_unica_activa_upd$$
DROP TRIGGER IF EXISTS trg_estancia_unica_activa_estado_upd$$

CREATE TRIGGER trg_estancia_habitacion_unica_activa_ins
BEFORE INSERT ON estancia_habitaciones
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM estancia_habitaciones eh
        JOIN estancias e ON e.id = eh.id_estancia
        WHERE eh.id_habitacion = NEW.id_habitacion
          AND e.estado IN ('ACTIVA', 'EXCEDIDA')
          AND eh.id_estancia <> NEW.id_estancia
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No puede existir mas de una estancia ACTIVA/EXCEDIDA por habitacion';
END IF;
END$$

CREATE TRIGGER trg_estancia_habitacion_unica_activa_upd
BEFORE UPDATE ON estancia_habitaciones
FOR EACH ROW
BEGIN
    IF EXISTS (
        SELECT 1
        FROM estancia_habitaciones eh
        JOIN estancias e ON e.id = eh.id_estancia
        WHERE eh.id_habitacion = NEW.id_habitacion
          AND e.estado IN ('ACTIVA', 'EXCEDIDA')
          AND eh.id_estancia <> NEW.id_estancia
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'No puede existir mas de una estancia ACTIVA/EXCEDIDA por habitacion';
END IF;
END$$

CREATE TRIGGER trg_estancia_unica_activa_estado_upd
BEFORE UPDATE ON estancias
FOR EACH ROW
BEGIN
    IF NEW.estado IN ('ACTIVA', 'EXCEDIDA') THEN
        IF EXISTS (
            SELECT 1
            FROM estancia_habitaciones eh_new
            JOIN estancia_habitaciones eh_other
              ON eh_other.id_habitacion = eh_new.id_habitacion
             AND eh_other.id_estancia <> eh_new.id_estancia
            JOIN estancias e_other
              ON e_other.id = eh_other.id_estancia
            WHERE eh_new.id_estancia = NEW.id
              AND e_other.estado IN ('ACTIVA', 'EXCEDIDA')
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'No puede activar/exceder una estancia si ya existe otra ACTIVA/EXCEDIDA en la habitacion';
        END IF;
    END IF;
END$$
