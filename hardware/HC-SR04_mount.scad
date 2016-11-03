//HC-SR04 mount

ex=0.1;         //utility
hole_dia=3;   //hole diameter
hole_offset=1.8;  //hole center offset from the board edge
base_height=2.5;  //mount base height
base_length=45.5;
base_width=20.3;
holder_width=2;
holder_length=10;
holder_height=10;
holder_offset=5;
inner_offset=5;

main();

module main(){
    difference(){
        translate([-holder_width, -holder_width, 0]) 
            cube([base_length+2*holder_width, base_width+2*holder_width, base_height]);
        holes();
        translate([inner_offset, inner_offset, -ex]) cube([base_length-2*inner_offset, base_width-2*inner_offset, base_height+2*ex]);
    }
    holders();
}

module holes(){
    translate([hole_offset, hole_offset, -ex]) hole();
    translate([base_length-hole_offset, hole_offset, -ex]) hole();
    translate([base_length-hole_offset, base_width-hole_offset, -ex]) hole();
    translate([hole_offset, base_width-hole_offset, -ex]) hole();
}

module hole(){
    union(){
        cylinder(d=hole_dia, h=base_height+2*ex, $fn=25);
        M2nut();
    }
}

module M2nut(nutHeight=1){
    translate([0,0, base_height-nutHeight]) 
        cylinder(r = 4 / 2 / cos(180 / 6) + 2*ex, h=nutHeight+2*ex, $fn=6);
}

module holders(){
    translate([holder_offset, -holder_width, 0]) holder();
    translate([base_length-holder_length-holder_offset, -holder_width, 0]) holder();
    translate([base_length-holder_length-holder_offset, base_width, 0]) holder();
    translate([holder_offset, base_width, 0]) holder();
}

module holder(){
    cube([holder_length, holder_width, holder_height]);
}