<?php
    $con = mysqli_connect("localhost", "root", "", "mobilesilencer");

    $query = "SELECT * FROM mosque_info";

    $result = mysqli_query($con, $query);
   
    while($res = mysqli_fetch_array($result))
    {
        $data[] = $res;
    }
    print(json_encode($data));
?>